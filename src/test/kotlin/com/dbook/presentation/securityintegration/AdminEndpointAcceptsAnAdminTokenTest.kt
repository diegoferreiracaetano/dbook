package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test

class AdminEndpointAcceptsAnAdminTokenTest : SecurityIntegrationFixture() {
    @Test
    fun `given an ADMIN token when posting to admin flights then it returns 201`() {
        val token = registerAdminAndLogin(uniqueEmail())

        mockMvc.post("/admin/flights") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = validFlightRequestBody()
        }.andExpect { status { isCreated() } }
    }
}
