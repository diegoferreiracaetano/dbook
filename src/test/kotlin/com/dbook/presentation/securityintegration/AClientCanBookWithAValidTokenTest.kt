package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test

class AClientCanBookWithAValidTokenTest : SecurityIntegrationFixture() {
    @Test
    fun `given a valid CLIENT token when posting a booking then it returns 201`() {
        val token = registerAndLogin(uniqueEmail())
        val bookableId = registerFlightWithOneSeat()

        mockMvc.post("/bookings") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("bookableId" to bookableId))
        }.andExpect { status { isCreated() } }
    }
}
