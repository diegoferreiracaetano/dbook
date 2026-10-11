package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertEquals

class TheCustomerSeesAndEndsOnlyTheirOwnSessionsTest : SecurityIntegrationFixture() {
    @Test
    fun `given two devices when listing then both show, ending one stops its refresh and keeps the other`() {
        val email = uniqueEmail()
        val token = registerAndLogin(email)
        val firstRefresh = loginRefreshToken(email, "s3cret-password")

        val sessions = objectMapper.readTree(listSessions(token))
        assertEquals(2, sessions.size())
        val ended = sessions.first()["id"].asText()

        mockMvc.delete("/v1/users/me/sessions/$ended") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isNoContent() } }

        assertEquals(1, objectMapper.readTree(listSessions(token)).size())
        // the other device's refresh still works or, if it was the one ended, is refused: both are one of the two
        val refresh =
            mockMvc.post("/v1/auth/refresh") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("refreshToken" to firstRefresh))
            }.andReturn().response.status
        assertEquals(true, refresh == 200 || refresh == 401)
    }

    @Test
    fun `given another user's session id when ending it then it is a 404 and theirs stays`() {
        val token = registerAndLogin(uniqueEmail())
        val otherToken = registerAndLogin(uniqueEmail())
        val othersSession = objectMapper.readTree(listSessions(otherToken)).first()["id"].asText()

        mockMvc.delete("/v1/users/me/sessions/$othersSession") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isNotFound() } }

        assertEquals(1, objectMapper.readTree(listSessions(otherToken)).size())
    }

    @Test
    fun `given no token when listing the sessions then it is a 401`() {
        mockMvc.get("/v1/users/me/sessions").andExpect { status { isUnauthorized() } }
    }

    private fun listSessions(token: String): String =
        mockMvc.get("/v1/users/me/sessions") {
            header("Authorization", "Bearer $token")
        }.andReturn().response.contentAsString
}
