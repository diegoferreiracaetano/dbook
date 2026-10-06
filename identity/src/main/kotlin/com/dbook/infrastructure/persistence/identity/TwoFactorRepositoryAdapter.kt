package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.TotpEnrollment
import com.dbook.domain.identity.TwoFactorRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant

// Plain SQL: every decision here is one conditional statement ("only if newer", "only if unspent"), and the number of
// rows it touched is the answer: two requests with the same code cannot both win.
@Repository
class TwoFactorRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : TwoFactorRepository {
    @Transactional(readOnly = true)
    override fun find(userId: Long): TotpEnrollment? =
        jdbc.query(
            "SELECT user_id, secret_encrypted, confirmed_at, last_used_step FROM staff_totp WHERE user_id = :user",
            mapOf("user" to userId),
        ) { rs, _ ->
            TotpEnrollment(
                userId = rs.getLong("user_id"),
                secretEncrypted = rs.getString("secret_encrypted"),
                confirmedAt = rs.getTimestamp("confirmed_at")?.toInstant(),
                lastUsedStep = rs.getLong("last_used_step"),
            )
        }.firstOrNull()

    // an unconfirmed enrollment is replaced; a confirmed one is left alone (the WHERE of the conflict)
    @Transactional
    override fun savePending(
        userId: Long,
        secretEncrypted: String,
    ) {
        jdbc.update(
            "INSERT INTO staff_totp (user_id, secret_encrypted) VALUES (:user, :secret) " +
                "ON CONFLICT (user_id) DO UPDATE SET secret_encrypted = :secret, created_at = now() " +
                "WHERE staff_totp.confirmed_at IS NULL",
            mapOf("user" to userId, "secret" to secretEncrypted),
        )
    }

    @Transactional
    override fun confirm(
        userId: Long,
        step: Long,
        now: Instant,
        recoveryCodeHashes: List<String>,
    ): Boolean {
        val activated =
            jdbc.update(
                "UPDATE staff_totp SET confirmed_at = :now, last_used_step = :step " +
                    "WHERE user_id = :user AND confirmed_at IS NULL",
                MapSqlParameterSource("now", Timestamp.from(now)).addValue("step", step).addValue("user", userId),
            ) == 1
        if (activated) {
            jdbc.update("DELETE FROM staff_recovery_code WHERE user_id = :user", mapOf("user" to userId))
            recoveryCodeHashes.forEach { hash ->
                jdbc.update(
                    "INSERT INTO staff_recovery_code (user_id, code_hash) VALUES (:user, :hash)",
                    mapOf("user" to userId, "hash" to hash),
                )
            }
        }
        return activated
    }

    @Transactional
    override fun advanceStep(
        userId: Long,
        step: Long,
    ): Boolean =
        jdbc.update(
            "UPDATE staff_totp SET last_used_step = :step " +
                "WHERE user_id = :user AND confirmed_at IS NOT NULL AND last_used_step < :step",
            mapOf("user" to userId, "step" to step),
        ) == 1

    @Transactional
    override fun spendRecoveryCode(
        userId: Long,
        hash: String,
        now: Instant,
    ): Boolean =
        jdbc.update(
            "UPDATE staff_recovery_code SET used_at = :now " +
                "WHERE user_id = :user AND code_hash = :hash AND used_at IS NULL",
            mapOf("now" to Timestamp.from(now), "user" to userId, "hash" to hash),
        ) == 1

    @Transactional(readOnly = true)
    override fun unspentRecoveryCodes(userId: Long): Int =
        jdbc.queryForObject(
            "SELECT count(*) FROM staff_recovery_code WHERE user_id = :user AND used_at IS NULL",
            mapOf("user" to userId),
            Int::class.javaObjectType,
        ) ?: 0

    @Transactional
    override fun remove(userId: Long) {
        jdbc.update("DELETE FROM staff_recovery_code WHERE user_id = :user", mapOf("user" to userId))
        jdbc.update("DELETE FROM staff_totp WHERE user_id = :user", mapOf("user" to userId))
    }
}
