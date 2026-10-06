package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.AccountToken
import com.dbook.domain.identity.AccountTokenPurpose
import com.dbook.domain.identity.AccountTokenRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant

// Plain SQL: spending a link is one conditional UPDATE and the number of rows it touched is the answer, so two
// requests with the same link cannot both win.
@Repository
class AccountTokenRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : AccountTokenRepository {
    @Transactional
    override fun issue(token: AccountToken): AccountToken {
        jdbc.update(
            "UPDATE account_token SET used_at = :now WHERE user_id = :user AND purpose = :purpose AND used_at IS NULL",
            mapOf("now" to Timestamp.from(token.createdAt), "user" to token.userId, "purpose" to token.purpose.name),
        )
        val keys = GeneratedKeyHolder()
        jdbc.update(
            "INSERT INTO account_token (user_id, purpose, token_hash, expires_at, created_at) " +
                "VALUES (:user, :purpose, :hash, :expires, :created)",
            MapSqlParameterSource("user", token.userId).addValue("purpose", token.purpose.name)
                .addValue("hash", token.tokenHash).addValue("expires", Timestamp.from(token.expiresAt))
                .addValue("created", Timestamp.from(token.createdAt)),
            keys,
            arrayOf("id"),
        )
        return token.copy(id = keys.key?.toLong())
    }

    @Transactional(readOnly = true)
    override fun findByTokenHash(tokenHash: String): AccountToken? =
        jdbc.query(
            "SELECT id, user_id, purpose, token_hash, expires_at, created_at, used_at FROM account_token " +
                "WHERE token_hash = :hash",
            mapOf("hash" to tokenHash),
        ) { rs, _ ->
            AccountToken(
                id = rs.getLong("id"),
                userId = rs.getLong("user_id"),
                purpose = AccountTokenPurpose.valueOf(rs.getString("purpose")),
                tokenHash = rs.getString("token_hash"),
                expiresAt = rs.getTimestamp("expires_at").toInstant(),
                createdAt = rs.getTimestamp("created_at").toInstant(),
                usedAt = rs.getTimestamp("used_at")?.toInstant(),
            )
        }.firstOrNull()

    @Transactional
    override fun consume(
        id: Long,
        now: Instant,
    ): Boolean =
        jdbc.update(
            "UPDATE account_token SET used_at = :now WHERE id = :id AND used_at IS NULL AND expires_at > :now",
            mapOf("now" to Timestamp.from(now), "id" to id),
        ) == 1
}
