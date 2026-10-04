package com.dbook.presentation.securityintegration

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class AdminLoginOpensTheSessionWithAnHttpOnlyCookieTest : SecurityIntegrationFixture() {
    @Test
    fun `given a staff member when logging into the portal then the refresh token is only in a locked cookie`() {
        val email = uniqueEmail()
        registerStaff(email)

        val result = adminLogin(email)

        assertEquals(200, result.response.status)
        val body = objectMapper.readTree(result.response.contentAsString)
        assertNotNull(body["accessToken"])
        assertFalse(body.has("refreshToken"), "the refresh token must only travel in the cookie")
        val cookie = requireNotNull(result.response.getHeader("Set-Cookie"))
        listOf("HttpOnly", "Secure", "SameSite=Strict", "Path=/v1/admin/auth").forEach { assertContains(cookie, it) }
    }
}
