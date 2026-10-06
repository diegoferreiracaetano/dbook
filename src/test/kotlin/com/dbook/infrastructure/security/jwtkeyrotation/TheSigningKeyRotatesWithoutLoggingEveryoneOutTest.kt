package com.dbook.infrastructure.security.jwtkeyrotation

import com.dbook.domain.common.access.Role
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

class TheSigningKeyRotatesWithoutLoggingEveryoneOutTest {
    // the real clock: the parser checks `exp` against it, so a token minted in the past would already be expired
    private val clock = Clock.systemUTC()
    private val user = User(id = 7, email = "a@example.com", passwordHash = "x", name = "A", role = Role.CLIENT)

    private fun secret(seed: Int) = Base64.getEncoder().encodeToString(ByteArray(32) { (it + seed).toByte() })

    private fun service(
        active: String,
        activeId: String,
        retired: Map<String, String> = emptyMap(),
    ) = JwtTokenService(active, 15, 7, clock, JwtKeyProperties(activeId, retired))

    @Test
    fun `given a token from before the rotation when its key is retired then it verifies, new ones use the new key`() {
        val before = service(secret(1), "k1").generateAccessToken(user)

        val after = service(secret(2), "k2", retired = mapOf("k1" to secret(1)))

        assertEquals(7L, after.parseUserId(before))
        val fresh = after.generateAccessToken(user)
        assertEquals(7L, after.parseUserId(fresh))
        assertNull(service(secret(1), "k1").parseUserId(fresh), "the old service does not know the new key")
    }

    @Test
    fun `given the retired key is dropped when an old token arrives then it is refused`() {
        val old = service(secret(1), "k1").generateAccessToken(user)

        assertNull(service(secret(2), "k2").parseUserId(old))
    }

    @Test
    fun `given a token naming a key nobody has or with a forged signature when parsed then it is refused`() {
        val service = service(secret(1), "k1")
        val unknownKid = service(secret(1), "k9").generateAccessToken(user)
        val forged = service.generateAccessToken(user).let { it.dropLast(3) + "abc" }

        assertNull(service.parseUserId(unknownKid))
        assertNull(service.parseUserId(forged))
    }

    @Test
    fun `given a token from before rotation existed with no kid when parsed then the active key checks it`() {
        val key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret(1)))
        val legacy =
            Jwts.builder().subject("7").claim("role", "CLIENT").expiration(Date.from(clock.instant().plusSeconds(60)))
                .signWith(key).compact()

        assertEquals(7L, service(secret(1), "k1").parseUserId(legacy))
        assertNull(service(secret(2), "k2").parseUserId(legacy))
        assertEquals(7L, service(secret(2), "k2", retired = mapOf("legacy" to secret(1))).parseUserId(legacy))
    }
}
