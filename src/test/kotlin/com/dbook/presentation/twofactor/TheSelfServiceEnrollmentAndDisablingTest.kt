package com.dbook.presentation.twofactor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TheSelfServiceEnrollmentAndDisablingTest : TwoFactorApiFixture() {
    private val email = uniqueEmail()

    @Test
    fun `given a signed in staff member when they enroll then nothing is active until the first code`() {
        val token = registerStaffAndLogin(email)

        val start = postJson("/v1/admin/2fa/enroll", emptyMap(), token)

        assertEquals(200, start.response.status)
        assertTrue(json(start)["otpauthUri"].asText().startsWith("otpauth://totp/DBook:"))
        assertEquals(200, adminLogin(email).response.status, "the password alone still opens the session")
        assertEquals(400, postJson("/v1/admin/2fa/confirm", mapOf("code" to "000000"), token).response.status)
        assertEquals(
            10,
            json(
                postJson("/v1/admin/2fa/confirm", mapOf("code" to codeOnThePhone(email)), token),
            )["recoveryCodes"].size(),
        )
    }

    @Test
    fun `given the second factor on when it is confirmed again or enrolled again then 409`() {
        staffWithTwoFactor(email)
        val token = json(verify(challengeFor(email), codeOnThePhone(email)))["accessToken"].asText()

        assertEquals(409, postJson("/v1/admin/2fa/enroll", emptyMap(), token).response.status)
        assertEquals(
            409,
            postJson("/v1/admin/2fa/confirm", mapOf("code" to codeOnThePhone(email)), token).response.status,
        )
    }

    @Test
    fun `given the password and a code when disabling then 204 and the password alone works again`() {
        staffWithTwoFactor(email)
        val token = json(verify(challengeFor(email), codeOnThePhone(email)))["accessToken"].asText()

        val off =
            postJson(
                "/v1/admin/2fa/disable",
                mapOf("password" to "s3cret-password", "code" to codeOnThePhone(email)),
                token,
            )

        assertEquals(204, off.response.status)
        assertEquals(200, adminLogin(email).response.status)
    }

    @Test
    fun `given a wrong password or code when disabling then it is refused and the second factor stays`() {
        staffWithTwoFactor(email)
        val token = json(verify(challengeFor(email), codeOnThePhone(email)))["accessToken"].asText()

        assertEquals(
            401,
            postJson(
                "/v1/admin/2fa/disable",
                mapOf("password" to "wrong", "code" to codeOnThePhone(email)),
                token,
            ).response.status,
        )
        assertEquals(
            400,
            postJson(
                "/v1/admin/2fa/disable",
                mapOf("password" to "s3cret-password", "code" to "000000"),
                token,
            ).response.status,
        )
        assertEquals(202, adminLogin(email).response.status)
    }

    @Test
    fun `given no bearer when the 2FA endpoints are called then 401, and a customer gets 403`() {
        assertEquals(401, postJson("/v1/admin/2fa/enroll", emptyMap()).response.status)
        val customer = registerAndLogin(uniqueEmail())
        assertEquals(403, postJson("/v1/admin/2fa/enroll", emptyMap(), customer).response.status)
    }
}
