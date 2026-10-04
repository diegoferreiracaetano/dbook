package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertEquals

class ABlockedAccountGetsAForbiddenWithItsCodeAndNoSessionTest : SecurityIntegrationFixture() {
    @Test
    fun `given a blocked customer when logging in then 403 ACCOUNT_BLOCKED, and old sessions cannot refresh`() {
        val email = uniqueEmail()
        registerAndLogin(email)
        val refreshToken = loginRefreshToken(email, "s3cret-password")

        block(email)

        val login =
            mockMvc.post("/v1/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("email" to email, "password" to "s3cret-password"))
            }.andReturn()
        assertEquals(403, login.response.status)
        assertEquals("ACCOUNT_BLOCKED", errorCodeOf(login))
        mockMvc.post("/v1/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("refreshToken" to refreshToken))
        }.andExpect { status { isUnauthorized() } }
    }
}
