package com.dbook.presentation.twofactor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TheClientLoginIsNotADoorAroundTheSecondFactorTest : TwoFactorApiFixture() {
    private fun clientLogin(email: String) =
        postJson("/v1/auth/login", mapOf("email" to email, "password" to "s3cret-password"))

    @Test
    fun `given staff with the second factor when they use the client login then 403 and no token`() {
        val email = uniqueEmail()
        staffWithTwoFactor(email)

        val result = clientLogin(email)

        assertEquals(403, result.response.status)
        assertEquals("TWO_FACTOR_REQUIRED", errorCodeOf(result))
        assertEquals(false, json(result).has("accessToken"))
    }

    @Test
    fun `given staff without one and a customer when they use the client login then it works as before`() {
        val staff = uniqueEmail()
        registerStaff(staff)
        val customer = uniqueEmail()
        registerAndLogin(customer)

        assertNotNull(json(clientLogin(staff))["accessToken"].asText())
        assertNotNull(json(clientLogin(customer))["accessToken"].asText())
    }
}
