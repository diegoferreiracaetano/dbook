package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class LoginIsLockedAfterTooManyFailuresTest : SecurityIntegrationFixture() {
    @Test
    fun `given five failed logins for an email when trying again then it is 429 with Retry-After`() {
        val email = uniqueEmail()
        registerAndLogin(email)
        repeat(5) { assertEquals(401, adminLogin(email, "wrong-password-here").response.status) }

        val locked =
            mockMvc.post("/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("email" to email, "password" to "s3cret-password"))
            }.andReturn()

        assertEquals(429, locked.response.status)
        assertNotNull(locked.response.getHeader("Retry-After"))
        assertEquals("TOO_MANY_ATTEMPTS", errorCodeOf(locked))
    }
}
