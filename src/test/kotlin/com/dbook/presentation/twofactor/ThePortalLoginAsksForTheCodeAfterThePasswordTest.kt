package com.dbook.presentation.twofactor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThePortalLoginAsksForTheCodeAfterThePasswordTest : TwoFactorApiFixture() {
    private val email = uniqueEmail()

    @Test
    fun `given an account with the second factor when the password is right then 202 and no session`() {
        staffWithTwoFactor(email)

        val login = adminLogin(email)

        assertEquals(202, login.response.status)
        assertNotNull(json(login)["challengeToken"].asText())
        assertEquals(false, json(login)["enrollmentRequired"].asBoolean())
        assertNull(login.response.getHeader("Set-Cookie"), "no refresh cookie before the code")
        assertTrue(json(login).has("accessToken").not())
    }

    @Test
    fun `given the challenge when the code on the phone is sent then the session opens and the cookie is set`() {
        staffWithTwoFactor(email)
        val challenge = challengeFor(email)

        val session = verify(challenge, codeOnThePhone(email))

        assertEquals(200, session.response.status)
        assertNotNull(session.response.getHeader("Set-Cookie"))
        val me = getWith("/v1/admin/auth/me", json(session)["accessToken"].asText())
        assertEquals(200, me.response.status)
        assertEquals(true, json(me)["twoFactorEnabled"].asBoolean())
    }

    @Test
    fun `given a wrong code when verifying then 400 with its code and no session`() {
        staffWithTwoFactor(email)
        val challenge = challengeFor(email)

        val wrong = verify(challenge, "000000")

        assertEquals(400, wrong.response.status)
        assertEquals("INVALID_TWO_FACTOR_CODE", errorCodeOf(wrong))
        assertNull(wrong.response.getHeader("Set-Cookie"))
    }

    @Test
    fun `given a code that already opened a session when it is sent again then it is refused as a replay`() {
        staffWithTwoFactor(email)
        val code = codeOnThePhone(email)
        assertEquals(200, verify(challengeFor(email), code).response.status)

        val replay = verify(challengeFor(email), code)

        assertEquals(400, replay.response.status)
        assertEquals("INVALID_TWO_FACTOR_CODE", errorCodeOf(replay))
    }

    @Test
    fun `given a recovery code when verifying then the session opens once and the code is spent`() {
        val codes = staffWithTwoFactor(email)

        assertEquals(200, verify(challengeFor(email), codes.first()).response.status)
        assertEquals(400, verify(challengeFor(email), codes.first()).response.status)
    }

    @Test
    fun `given the secret in the database when read then it is encrypted and the recovery codes are hashes`() {
        val codes = staffWithTwoFactor(email)
        val userId = userIdOf(email)

        val stored =
            jdbcTemplate.queryForObject(
                "SELECT secret_encrypted FROM staff_totp WHERE user_id = ?",
                String::class.java,
                userId,
            )
        val hashes =
            jdbcTemplate.queryForList(
                "SELECT code_hash FROM staff_recovery_code WHERE user_id = ?",
                String::class.java,
                userId,
            )

        assertTrue(requireNotNull(stored).startsWith("v1:"))
        assertEquals(10, hashes.size)
        assertTrue(hashes.none { it in codes }, "a recovery code is never stored as it was shown")
    }
}
