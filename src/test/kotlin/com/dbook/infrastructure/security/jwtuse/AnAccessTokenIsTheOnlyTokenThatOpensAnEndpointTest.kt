package com.dbook.infrastructure.security.jwtuse

import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.ChallengePurpose
import com.dbook.domain.identity.TwoFactorChallenge
import com.dbook.domain.identity.User
import com.dbook.infrastructure.security.JwtKeyProperties
import com.dbook.infrastructure.security.JwtTokenService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.time.Clock
import java.util.Base64
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// A refresh token lives for days and a challenge token proves only that the password was right: neither may open an
// endpoint as if it were an access token.
class AnAccessTokenIsTheOnlyTokenThatOpensAnEndpointTest {
    private val secret = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
    private val service =
        JwtTokenService(secret, 15, 7, Clock.systemUTC(), JwtKeyProperties(activeKeyId = "k1", retired = emptyMap()))
    private val user = User(id = 7, email = "a@example.com", passwordHash = "x", name = "A", role = Role.SUPER_ADMIN)

    @Test
    fun `given an access token when parsed then the user and the role come out`() {
        val token = service.generateAccessToken(user)

        assertEquals(7L, service.parseUserId(token))
        assertEquals(Role.SUPER_ADMIN, service.parseRole(token))
    }

    @Test
    fun `given a refresh token when used as an access token then it opens nothing`() {
        val token = service.generateRefreshToken(user)

        assertNull(service.parseUserId(token))
        assertNull(service.parseRole(token))
    }

    @Test
    fun `given a challenge token when used as an access token then it opens nothing`() {
        val token = service.generateChallengeToken(7, ChallengePurpose.VERIFY)

        assertNull(service.parseUserId(token))
        assertNull(service.parseRole(token))
    }

    @Test
    fun `given a challenge token when parsed then it says who and what comes next`() {
        assertEquals(
            TwoFactorChallenge(7, ChallengePurpose.VERIFY),
            service.parseChallenge(service.generateChallengeToken(7, ChallengePurpose.VERIFY)),
        )
        assertEquals(
            TwoFactorChallenge(7, ChallengePurpose.ENROLL),
            service.parseChallenge(service.generateChallengeToken(7, ChallengePurpose.ENROLL)),
        )
    }

    @Test
    fun `given access and refresh tokens when parsed as a challenge then they are not one`() {
        assertNull(service.parseChallenge(service.generateAccessToken(user)))
        assertNull(service.parseChallenge(service.generateRefreshToken(user)))
        assertNull(service.parseChallenge("not-a-token"))
    }

    @Test
    fun `given a token from before the use claim existed when parsed then it still counts as an access token`() {
        val legacy =
            Jwts.builder()
                .subject("7")
                .claim("role", "SUPER_ADMIN")
                .expiration(Date(System.currentTimeMillis() + 60_000))
                .header().keyId("k1").and()
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret)))
                .compact()

        assertEquals(7L, service.parseUserId(legacy))
        assertNull(service.parseChallenge(legacy))
    }
}
