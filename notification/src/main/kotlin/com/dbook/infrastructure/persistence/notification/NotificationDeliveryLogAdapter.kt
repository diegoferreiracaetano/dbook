package com.dbook.infrastructure.persistence.notification

import com.dbook.domain.notification.DeliveryClaim
import com.dbook.domain.notification.NotificationChannel
import com.dbook.domain.notification.NotificationDeliveryLog
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Clock
import java.util.UUID

@Repository
class NotificationDeliveryLogAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val clock: Clock,
) : NotificationDeliveryLog {
    // One statement decides it: a new row is NEW; an existing PENDING one (a crash or a failure) is bumped and is a
    // RETRY; one already SENT or SKIPPED is not touched by the WHERE, returns nothing, and is ALREADY_DONE.
    @Transactional
    override fun claim(
        eventId: UUID,
        channel: NotificationChannel,
    ): DeliveryClaim {
        val inserted =
            jdbc.query(
                "INSERT INTO notification_delivery (event_id, channel, status, attempts, created_at, updated_at) " +
                    "VALUES (:event, :channel, 'PENDING', 1, :now, :now) " +
                    "ON CONFLICT (event_id, channel) DO UPDATE SET attempts = notification_delivery.attempts + 1, " +
                    "updated_at = :now WHERE notification_delivery.status = 'PENDING' RETURNING (xmax = 0) AS inserted",
                params(eventId, channel),
            ) { rs, _ -> rs.getBoolean("inserted") }
        return when (inserted.firstOrNull()) {
            true -> DeliveryClaim.NEW
            false -> DeliveryClaim.RETRY
            null -> DeliveryClaim.ALREADY_DONE
        }
    }

    @Transactional
    override fun markSent(
        eventId: UUID,
        channel: NotificationChannel,
    ) = finish(eventId, channel, "SENT")

    @Transactional
    override fun markSkipped(
        eventId: UUID,
        channel: NotificationChannel,
    ) = finish(eventId, channel, "SKIPPED")

    @Transactional
    override fun markFailed(
        eventId: UUID,
        channel: NotificationChannel,
        error: String,
    ) {
        jdbc.update(
            "UPDATE notification_delivery SET last_error = :error, updated_at = :now " +
                "WHERE event_id = :event AND channel = :channel",
            params(eventId, channel).addValue("error", error.take(MAX_ERROR_LENGTH)),
        )
    }

    private fun finish(
        eventId: UUID,
        channel: NotificationChannel,
        status: String,
    ) {
        jdbc.update(
            "UPDATE notification_delivery SET status = :status, last_error = NULL, updated_at = :now " +
                "WHERE event_id = :event AND channel = :channel",
            params(eventId, channel).addValue("status", status),
        )
    }

    private fun params(
        eventId: UUID,
        channel: NotificationChannel,
    ) = MapSqlParameterSource()
        .addValue("event", eventId)
        .addValue("channel", channel.name)
        .addValue("now", Timestamp.from(clock.instant()))

    private companion object {
        const val MAX_ERROR_LENGTH = 500
    }
}
