package com.dbook.presentation.twofactor

import com.dbook.domain.common.access.Role
import org.springframework.test.context.TestPropertySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// the policy on: SUPER_ADMIN cannot reach the portal without an authenticator (the other tests run with it off)
@TestPropertySource(properties = ["admin.two-factor.required-roles=SUPER_ADMIN"])
class ARequiredRoleEnrollsAtTheFirstLoginTest : TwoFactorApiFixture() {
    private val email = uniqueEmail()

    @Test
    fun `given a SUPER_ADMIN without an authenticator when they sign in then they must enroll and no session opens`() {
        registerStaff(email)

        val login = adminLogin(email)

        assertEquals(202, login.response.status)
        assertEquals(true, json(login)["enrollmentRequired"].asBoolean())
        assertEquals(null, login.response.getHeader("Set-Cookie"))
    }

    @Test
    fun `given the enrollment challenge when the first code is confirmed then the session opens`() {
        registerStaff(email)
        val challenge = challengeFor(email)

        val start = postJson("/v1/admin/auth/2fa/enroll", mapOf("challengeToken" to challenge))
        assertTrue(json(start)["otpauthUri"].asText().startsWith("otpauth://totp/"))
        val done =
            postJson(
                "/v1/admin/auth/2fa/confirm",
                mapOf("challengeToken" to challenge, "code" to codeOnThePhone(email)),
            )

        assertEquals(200, done.response.status)
        assertNotNull(done.response.getHeader("Set-Cookie"))
        assertEquals(10, json(done)["recoveryCodes"].size())
        assertEquals(200, getWith("/v1/admin/auth/me", json(done)["accessToken"].asText()).response.status)
    }

    @Test
    fun `given a wrong first code when confirming then 400 and no session`() {
        registerStaff(email)
        val challenge = challengeFor(email)
        postJson("/v1/admin/auth/2fa/enroll", mapOf("challengeToken" to challenge))

        val done = postJson("/v1/admin/auth/2fa/confirm", mapOf("challengeToken" to challenge, "code" to "000000"))

        assertEquals(400, done.response.status)
        assertEquals(null, done.response.getHeader("Set-Cookie"))
    }

    @Test
    fun `given a verify challenge when used to enroll or a made up one then 401`() {
        assertEquals(401, postJson("/v1/admin/auth/2fa/enroll", mapOf("challengeToken" to "made-up")).response.status)
    }

    @Test
    fun `given a role the policy does not require when it signs in then the password alone is enough`() {
        val manager = uniqueEmail()
        registerStaff(manager, Role.CATALOG_MANAGER)

        assertEquals(200, adminLogin(manager).response.status)
    }

    @Test
    fun `given a required role with the second factor on when it tries to turn it off then 409`() {
        val code = codeOnEnrollment()
        val token = json(verify(challengeFor(email), code))["accessToken"].asText()

        val off =
            postJson(
                "/v1/admin/2fa/disable",
                mapOf("password" to "s3cret-password", "code" to codeOnThePhone(email)),
                token,
            )

        assertEquals(409, off.response.status)
        assertEquals(true, twoFactor.find(userIdOf(email))?.isActive)
    }

    // enrolls through the forced path and returns a code for the next sign in
    private fun codeOnEnrollment(): String {
        registerStaff(email)
        val challenge = challengeFor(email)
        postJson("/v1/admin/auth/2fa/enroll", mapOf("challengeToken" to challenge))
        postJson("/v1/admin/auth/2fa/confirm", mapOf("challengeToken" to challenge, "code" to codeOnThePhone(email)))
        return codeOnThePhone(email)
    }
}
