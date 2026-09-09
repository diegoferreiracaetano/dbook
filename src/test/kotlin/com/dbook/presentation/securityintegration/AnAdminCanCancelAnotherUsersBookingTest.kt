package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test

class AnAdminCanCancelAnotherUsersBookingTest : SecurityIntegrationFixture() {
    @Test
    fun `given another user's booking when an admin cancels it then it returns 200`() {
        val ownerToken = registerAndLogin(uniqueEmail())
        val adminToken = registerAdminAndLogin(uniqueEmail())
        val bookableId = registerFlightWithOneSeat()

        val bookingResult =
            mockMvc.post("/bookings") {
                header("Authorization", "Bearer $ownerToken")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("bookableId" to bookableId))
            }.andReturn()
        val bookingId = objectMapper.readTree(bookingResult.response.contentAsString)["id"].asLong()

        mockMvc.post("/bookings/$bookingId/cancel") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect { status { isOk() } }
    }
}
