package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test

class AUsedRefreshTokenCannotBeReusedTest : SecurityIntegrationFixture() {
    @Test
    fun `given an already-used refresh token when refreshing again then it returns 401`() {
        val email = uniqueEmail()
        registerAndLogin(email)
        val refreshToken = loginRefreshToken(email, "s3cret-password")

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("refreshToken" to refreshToken))
        }.andExpect { status { isOk() } }

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("refreshToken" to refreshToken))
        }.andExpect { status { isUnauthorized() } }
    }
}
