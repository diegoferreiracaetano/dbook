package com.dbook.presentation.securityintegration

import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

class UnauthenticatedAndForbiddenResponsesCarryTheirCodesTest : SecurityIntegrationFixture() {
    @Test
    fun `given no token or a client token on an admin route then the bodies carry their codes`() {
        val unauthenticated = mockMvc.get("/v1/admin/auth/me").andReturn()
        val token = registerAndLogin(uniqueEmail())
        val forbidden = mockMvc.get("/v1/admin/auth/me") { header("Authorization", "Bearer $token") }.andReturn()

        assertEquals("UNAUTHORIZED", errorCodeOf(unauthenticated))
        assertEquals("FORBIDDEN", errorCodeOf(forbidden))
    }
}
