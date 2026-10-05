package com.dbook.infrastructure.security

import com.dbook.domain.identity.ChallengePurpose
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.TokenService
import com.dbook.domain.identity.TwoFactorChallenge
import com.dbook.domain.identity.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.LocatorAdapter
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Key
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

private const val CLAIM_USE = "use"
private const val USE_ACCESS = "access"

@Component
class JwtTokenService(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-token-expiration-minutes}") private val accessTokenExpirationMinutes: Long,
    @Value("\${jwt.refresh-token-expiration-days}") private val refreshTokenExpirationDays: Long,
    private val clock: Clock,
    private val keyProperties: JwtKeyProperties,
) : TokenService {
    private val signingKey: SecretKey = keyFrom(secret)

    // every key that can verify: the retired ones and the active one
    private val verificationKeys: Map<String, SecretKey> =
        keyProperties.retired.mapValues { keyFrom(it.value) } + (keyProperties.activeKeyId to signingKey)

    // the key a token names in its `kid` header; none named is a token from before rotation existed
    private val keyLocator =
        object : LocatorAdapter<Key>() {
            override fun locate(header: JwsHeader): Key? =
                header.keyId?.let { verificationKeys[it] } ?: verificationKeys[LEGACY_KEY_ID]
                    ?: signingKey.takeIf { header.keyId == null }
        }

    override fun generateAccessToken(user: User): String =
        buildToken(user, accessTokenExpirationMinutes, ChronoUnit.MINUTES, USE_ACCESS)

    override fun generateRefreshToken(user: User): String =
        buildToken(user, refreshTokenExpirationDays, ChronoUnit.DAYS, USE_REFRESH)

    // only an access token opens an endpoint: a refresh or a challenge token is refused here. A token with no `use`
    // is from before the claim existed and is still taken as an access token, until the last of them has expired.
    override fun parseUserId(token: String): Long? = parseClaims(token)?.takeIf(::isAccess)?.subject?.toLong()

    override fun parseRole(token: String): Role? =
        parseClaims(token)?.takeIf(::isAccess)?.get("role", String::class.java)?.let(Role::valueOf)

    override fun generateChallengeToken(
        userId: Long,
        purpose: ChallengePurpose,
    ): String {
        val now = clock.instant()
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(userId.toString())
            .claim(CLAIM_USE, challengeUse(purpose))
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(CHALLENGE_MINUTES, ChronoUnit.MINUTES)))
            .header().keyId(keyProperties.activeKeyId).and()
            .signWith(signingKey)
            .compact()
    }

    override fun parseChallenge(token: String): TwoFactorChallenge? =
        parseClaims(token)?.let { claims ->
            ChallengePurpose.entries.firstOrNull { challengeUse(it) == claims[CLAIM_USE] }
                ?.let { TwoFactorChallenge(claims.subject.toLong(), it) }
        }

    // returning null for a malformed/expired/tampered token IS the contract here
    // (see TokenService.parseUserId/parseRole docs) — nothing is actually swallowed.
    @Suppress("SwallowedException")
    private fun parseClaims(token: String): Claims? =
        try {
            Jwts.parser().keyLocator(keyLocator).build().parseSignedClaims(token).payload
        } catch (ex: JwtException) {
            null
        } catch (ex: IllegalArgumentException) {
            null
        }

    override fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return Base64.getEncoder().encodeToString(digest)
    }

    override fun refreshTokenExpiresAt(): Instant = clock.instant().plus(refreshTokenExpirationDays, ChronoUnit.DAYS)

    private fun buildToken(
        user: User,
        amount: Long,
        unit: ChronoUnit,
        use: String,
    ): String {
        val userId = requireNotNull(user.id) { "Cannot issue a token for a user that hasn't been persisted" }
        val now = clock.instant()
        return Jwts.builder()
            .id(UUID.randomUUID().toString()) // jti: guarantees uniqueness even for two
            // tokens issued for the same user within the same second (JWT timestamps
            // only have second-level granularity), which the refresh_token table relies
            // on via its UNIQUE(token_hash) constraint.
            .subject(userId.toString())
            .claim("role", user.role.name)
            .claim(CLAIM_USE, use)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(amount, unit)))
            .header().keyId(keyProperties.activeKeyId).and()
            .signWith(signingKey)
            .compact()
    }

    private companion object {
        const val LEGACY_KEY_ID = "legacy"
        const val USE_REFRESH = "refresh"
        const val CHALLENGE_MINUTES = 5L
    }
}

private fun keyFrom(base64: String): SecretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64))

private fun challengeUse(purpose: ChallengePurpose) = "2fa-${purpose.name.lowercase()}"

// only an access token opens an endpoint; no `use` at all is a token from before the claim existed
private fun isAccess(claims: Claims): Boolean = claims[CLAIM_USE] == null || claims[CLAIM_USE] == USE_ACCESS
