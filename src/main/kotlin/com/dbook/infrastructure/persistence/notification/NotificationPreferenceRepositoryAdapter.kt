package com.dbook.infrastructure.persistence.notification

import com.dbook.domain.notification.NotificationChannel
import com.dbook.domain.notification.NotificationPreference
import com.dbook.domain.notification.NotificationPreferenceRepository
import com.dbook.domain.notification.NotificationType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class NotificationPreferenceRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : NotificationPreferenceRepository {
    @Transactional(readOnly = true)
    override fun findChosen(userId: Long): List<NotificationPreference> =
        jdbc.query(
            "SELECT type, channel, enabled FROM notification_preference WHERE user_id = :userId",
            mapOf("userId" to userId),
        ) { rs, _ ->
            NotificationPreference(
                NotificationType.valueOf(rs.getString("type")),
                NotificationChannel.valueOf(rs.getString("channel")),
                rs.getBoolean("enabled"),
            )
        }

    @Transactional
    override fun save(
        userId: Long,
        preferences: List<NotificationPreference>,
    ) {
        preferences.forEach {
            jdbc.update(
                "INSERT INTO notification_preference (user_id, type, channel, enabled) " +
                    "VALUES (:userId, :type, :channel, :enabled) " +
                    "ON CONFLICT (user_id, type, channel) DO UPDATE SET enabled = EXCLUDED.enabled",
                mapOf(
                    "userId" to userId,
                    "type" to it.type.name,
                    "channel" to it.channel.name,
                    "enabled" to it.enabled,
                ),
            )
        }
    }
}
