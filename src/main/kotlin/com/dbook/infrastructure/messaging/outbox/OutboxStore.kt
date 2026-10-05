package com.dbook.infrastructure.messaging.outbox

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/** An event a relay holds, ready to be delivered: [attempts] counts this one. */
data class ClaimedOutboxEvent(
    val id: UUID,
    val type: String,
    val payload: String,
    val headers: Map<String, String>,
    val attempts: Int,
)

data class OutboxStats(
    val pending: Long,
    val overdueSeconds: Long,
)

/**
 * What a relay does to the table. Each call is its own short transaction, so no database lock is held while the
 * network is being called: an event is **claimed** (a lease: nobody else may take it until it runs out), delivered, and
 * then marked. A relay that dies in between simply lets the lease run out and the event is delivered again.
 */
@Repository
class OutboxStore(
    private val jdbc: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
) {
    /**
     * Up to [limit] events that are due, oldest first, each leased until [leaseUntil]. `FOR UPDATE SKIP LOCKED` lets
     * several relays (several instances of the application) claim at once without ever taking the same event.
     */
    @Transactional
    fun claim(
        now: Instant,
        limit: Int,
        leaseUntil: Instant,
    ): List<ClaimedOutboxEvent> =
        jdbc.query(
            """
            WITH due AS (
                SELECT id FROM outbox_event
                WHERE published_at IS NULL AND next_attempt_at <= :now
                ORDER BY next_attempt_at, created_at
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            UPDATE outbox_event e SET attempts = e.attempts + 1, next_attempt_at = :leaseUntil
            FROM due WHERE e.id = due.id
            RETURNING e.id, e.type, e.payload::text AS payload, e.headers::text AS headers, e.attempts
            """,
            MapSqlParameterSource()
                .addValue("now", Timestamp.from(now))
                .addValue("limit", limit)
                .addValue("leaseUntil", Timestamp.from(leaseUntil)),
        ) { rs, _ ->
            ClaimedOutboxEvent(
                id = rs.getObject("id", UUID::class.java),
                type = rs.getString("type"),
                payload = rs.getString("payload"),
                headers = objectMapper.readValue(rs.getString("headers"), HEADERS),
                attempts = rs.getInt("attempts"),
            )
        }

    @Transactional
    fun markPublished(
        id: UUID,
        now: Instant,
    ) {
        jdbc.update(
            "UPDATE outbox_event SET published_at = :now, last_error = NULL WHERE id = :id",
            mapOf("id" to id, "now" to Timestamp.from(now)),
        )
    }

    /** The delivery failed: try again at [retryAt], and keep why (for whoever looks at the table). */
    @Transactional
    fun markFailed(
        id: UUID,
        retryAt: Instant,
        error: String,
    ) {
        jdbc.update(
            "UPDATE outbox_event SET next_attempt_at = :retryAt, last_error = :error WHERE id = :id",
            mapOf("id" to id, "retryAt" to Timestamp.from(retryAt), "error" to error.take(MAX_ERROR_LENGTH)),
        )
    }

    /** Not yet published (including the ones scheduled for later), and how late the most overdue one is. */
    @Transactional(readOnly = true)
    fun stats(now: Instant): OutboxStats {
        val params = mapOf("now" to Timestamp.from(now))
        val pending =
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_event WHERE published_at IS NULL",
                params,
                Long::class.javaObjectType,
            ) ?: 0L
        val overdue =
            jdbc.queryForObject(
                "SELECT coalesce(extract(epoch FROM (:now - min(available_at)))::bigint, 0) FROM outbox_event " +
                    "WHERE published_at IS NULL AND available_at <= :now",
                params,
                Long::class.javaObjectType,
            ) ?: 0L
        return OutboxStats(pending, overdue)
    }

    /** Deletes what was published before [before]: the table is a queue, not an archive. */
    @Transactional
    fun deletePublishedBefore(before: Instant): Int =
        jdbc.update(
            "DELETE FROM outbox_event WHERE published_at IS NOT NULL AND published_at < :before",
            mapOf("before" to Timestamp.from(before)),
        )

    private companion object {
        const val MAX_ERROR_LENGTH = 500
        val HEADERS = object : TypeReference<Map<String, String>>() {}
    }
}
