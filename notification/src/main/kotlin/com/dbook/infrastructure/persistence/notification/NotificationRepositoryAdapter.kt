package com.dbook.infrastructure.persistence.notification

import com.dbook.domain.notification.Notification
import com.dbook.domain.notification.NotificationRepository
import com.dbook.domain.notification.NotificationType
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Repository
class NotificationRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
) : NotificationRepository {
    // the unique event_id answers "was this event already turned into a notification": a duplicate inserts nothing
    @Transactional
    override fun save(notification: Notification): Notification? {
        val id =
            jdbc.query(
                "INSERT INTO notification (user_id, type, title, body, data, event_id, read_at, created_at) " +
                    "VALUES (:userId, :type, :title, :body, CAST(:data AS jsonb), :eventId, :readAt, :createdAt) " +
                    "ON CONFLICT (event_id) DO NOTHING RETURNING id",
                MapSqlParameterSource()
                    .addValue("userId", notification.userId)
                    .addValue("type", notification.type.name)
                    .addValue("title", notification.title)
                    .addValue("body", notification.body)
                    .addValue("data", objectMapper.writeValueAsString(notification.data))
                    .addValue("eventId", notification.eventId)
                    .addValue("readAt", notification.readAt?.let(Timestamp::from))
                    .addValue("createdAt", Timestamp.from(notification.createdAt)),
            ) { rs, _ -> rs.getLong("id") }.firstOrNull()
        return id?.let { notification.copy(id = it) }
    }

    @Transactional(readOnly = true)
    override fun findByUser(
        userId: Long,
        beforeId: Long?,
        size: Int,
        unreadOnly: Boolean,
    ): List<Notification> {
        val conditions = mutableListOf("user_id = :userId")
        val params = MapSqlParameterSource("userId", userId).addValue("size", size)
        beforeId?.let {
            conditions += "id < :before"
            params.addValue("before", it)
        }
        if (unreadOnly) {
            conditions += "read_at IS NULL"
        }
        return jdbc.query(
            "SELECT id, user_id, type, title, body, data::text AS data, event_id, read_at, created_at " +
                "FROM notification WHERE ${conditions.joinToString(" AND ")} ORDER BY id DESC LIMIT :size",
            params,
        ) { rs, _ -> notificationOf(rs) }
    }

    @Transactional(readOnly = true)
    override fun countUnread(userId: Long): Long =
        jdbc.queryForObject(
            "SELECT count(*) FROM notification WHERE user_id = :userId AND read_at IS NULL",
            mapOf("userId" to userId),
            Long::class.javaObjectType,
        ) ?: 0L

    // coalesce: reading again keeps the first moment it was read
    @Transactional
    override fun markRead(
        id: Long,
        userId: Long,
        now: Instant,
    ): Boolean =
        jdbc.update(
            "UPDATE notification SET read_at = coalesce(read_at, :now) WHERE id = :id AND user_id = :userId",
            mapOf("id" to id, "userId" to userId, "now" to Timestamp.from(now)),
        ) > 0

    @Transactional
    override fun markAllRead(
        userId: Long,
        now: Instant,
    ): Int =
        jdbc.update(
            "UPDATE notification SET read_at = :now WHERE user_id = :userId AND read_at IS NULL",
            mapOf("userId" to userId, "now" to Timestamp.from(now)),
        )

    private fun notificationOf(rs: ResultSet) =
        Notification(
            id = rs.getLong("id"),
            userId = rs.getLong("user_id"),
            type = NotificationType.valueOf(rs.getString("type")),
            title = rs.getString("title"),
            body = rs.getString("body"),
            data = objectMapper.readValue(rs.getString("data"), DATA),
            eventId = rs.getObject("event_id", UUID::class.java),
            readAt = rs.getTimestamp("read_at")?.toInstant(),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )

    private companion object {
        val DATA = object : TypeReference<Map<String, Any?>>() {}
    }
}
