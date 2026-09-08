package com.dbook.infrastructure.security

import com.dbook.domain.Role
import com.dbook.domain.TokenService
import com.dbook.domain.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenService(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-token-expiration-minutes}") private val accessTokenExpirationMinutes: Long,
    @Value("\${jwt.refresh-token-expiration-days}") private val refreshTokenExpirationDays: Long,
) : TokenService {
    private val signingKey: SecretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))

    override fun generateAccessToken(user: User): String =
        buildToken(
            user,
            accessTokenExpirationMinutes,
            ChronoUnit.MINUTES,
        )

    override fun generateRefreshToken(user: User): String =
        buildToken(
            user,
            refreshTokenExpirationDays,
            ChronoUnit.DAYS,
        )

    override fun parseUserId(token: String): Long? = parseClaims(token)?.subject?.toLong()

    override fun parseRole(token: String): Role? =
        parseClaims(
            token,
        )?.get("role", String::class.java)?.let(Role::valueOf)

    // returning null for a malformed/expired/tampered token IS the contract here
    // (see TokenService.parseUserId/parseRole docs) — nothing is actually swallowed.
    @Suppress("SwallowedException")
    private fun parseClaims(token: String): Claims? =
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).payload
        } catch (ex: JwtException) {
            null
        } catch (ex: IllegalArgumentException) {
            null
        }

    override fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return Base64.getEncoder().encodeToString(digest)
    }

    override fun refreshTokenExpiresAt(): Instant = Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS)

    private fun buildToken(
        user: User,
        amount: Long,
        unit: ChronoUnit,
    ): String {
        val userId = requireNotNull(user.id) { "Cannot issue a token for a user that hasn't been persisted" }
        val now = Instant.now()
        return Jwts.builder()
            .id(UUID.randomUUID().toString()) // jti: guarantees uniqueness even for two
            // tokens issued for the same user within the same second (JWT timestamps
            // only have second-level granularity), which the refresh_token table relies
            // on via its UNIQUE(token_hash) constraint.
            .subject(userId.toString())
            .claim("role", user.role.name)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(amount, unit)))
            .signWith(signingKey)
            .compact()
    }
}
