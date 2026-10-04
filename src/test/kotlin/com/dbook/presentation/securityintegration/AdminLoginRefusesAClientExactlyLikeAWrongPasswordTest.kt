package com.dbook.presentation.securityintegration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdminLoginRefusesAClientExactlyLikeAWrongPasswordTest : SecurityIntegrationFixture() {
    @Test
    fun `given a client with the right password when logging into the portal then it looks like a wrong password`() {
        val email = uniqueEmail()
        registerAndLogin(email)

        val clientRightPassword = adminLogin(email, "s3cret-password")
        val clientWrongPassword = adminLogin(email, "wrong-password-here")

        assertEquals(401, clientRightPassword.response.status)
        assertEquals(clientWrongPassword.response.status, clientRightPassword.response.status)
        assertEquals(clientWrongPassword.response.contentAsString, clientRightPassword.response.contentAsString)
        assertNull(clientRightPassword.response.getHeader("Set-Cookie"))
    }
}
