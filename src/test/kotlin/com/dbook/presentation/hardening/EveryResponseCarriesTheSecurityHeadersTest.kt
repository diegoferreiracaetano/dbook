package com.dbook.presentation.hardening

import com.dbook.presentation.securityintegration.SecurityIntegrationFixture
import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EveryResponseCarriesTheSecurityHeadersTest : SecurityIntegrationFixture() {
    @Test
    fun `given an API response when read then it forbids loading, framing, sniffing and referrers`() {
        val response = mockMvc.get("/v1/destinations").andReturn().response

        assertEquals("default-src 'none'; frame-ancestors 'none'", response.getHeader("Content-Security-Policy"))
        assertEquals("no-referrer", response.getHeader("Referrer-Policy"))
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"))
        assertEquals("DENY", response.getHeader("X-Frame-Options"))
        assertEquals("geolocation=(), camera=(), microphone=()", response.getHeader("Permissions-Policy"))
    }

    @Test
    fun `given a secure request when read then HSTS is sent, and never over plain HTTP`() {
        val secure = mockMvc.get("/v1/destinations") { secure = true }.andReturn().response
        val plain = mockMvc.get("/v1/destinations").andReturn().response

        assertNotNull(secure.getHeader("Strict-Transport-Security"))
        assertNull(plain.getHeader("Strict-Transport-Security"))
    }

    @Test
    fun `given the Swagger UI when read then it has no CSP of 'none', it is a page that needs its own scripts`() {
        val response = mockMvc.get("/swagger-ui/index.html").andReturn().response

        assertNull(response.getHeader("Content-Security-Policy"))
    }
}
