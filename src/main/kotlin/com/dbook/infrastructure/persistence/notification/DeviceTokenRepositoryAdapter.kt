package com.dbook.infrastructure.persistence.notification

import com.dbook.domain.notification.DevicePlatform
import com.dbook.domain.notification.DeviceToken
import com.dbook.domain.notification.DeviceTokenRepository
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp

@Repository
class DeviceTokenRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : DeviceTokenRepository {
    @Transactional
    override fun register(deviceToken: DeviceToken) {
        jdbc.update(
            "INSERT INTO device_token (user_id, token, platform, last_seen_at) " +
                "VALUES (:userId, :token, :platform, :seen) " +
                "ON CONFLICT (token) DO UPDATE SET user_id = EXCLUDED.user_id, platform = EXCLUDED.platform, " +
                "last_seen_at = EXCLUDED.last_seen_at",
            mapOf(
                "userId" to deviceToken.userId,
                "token" to deviceToken.token,
                "platform" to deviceToken.platform.name,
                "seen" to Timestamp.from(deviceToken.lastSeenAt),
            ),
        )
    }

    @Transactional
    override fun remove(
        userId: Long,
        token: String,
    ) {
        jdbc.update(
            "DELETE FROM device_token WHERE user_id = :userId AND token = :token",
            mapOf("userId" to userId, "token" to token),
        )
    }

    @Transactional(readOnly = true)
    override fun findByUser(userId: Long): List<DeviceToken> =
        jdbc.query(
            "SELECT user_id, token, platform, last_seen_at FROM device_token WHERE user_id = :userId ORDER BY id",
            mapOf("userId" to userId),
        ) { rs, _ ->
            DeviceToken(
                rs.getLong("user_id"),
                rs.getString("token"),
                DevicePlatform.valueOf(rs.getString("platform")),
                rs.getTimestamp("last_seen_at").toInstant(),
            )
        }
}
