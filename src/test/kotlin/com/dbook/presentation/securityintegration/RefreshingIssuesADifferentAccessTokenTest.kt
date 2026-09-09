package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertNotEquals

class RefreshingIssuesADifferentAccessTokenTest : SecurityIntegrationFixture() {
    @Test
    fun `given a valid refresh token when refreshing then a new, different access token is issued`() {
        val email = uniqueEmail()
        registerAndLogin(email)
        val refreshToken = loginRefreshToken(email, "s3cret-password")
        val originalAccessToken = loginAccessToken(email, "s3cret-password")

        val refreshResult =
            mockMvc.post("/auth/refresh") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("refreshToken" to refreshToken))
            }.andReturn()
        val newAccessToken = objectMapper.readTree(refreshResult.response.contentAsString)["accessToken"].asText()

        assertNotEquals(originalAccessToken, newAccessToken)
    }
}
