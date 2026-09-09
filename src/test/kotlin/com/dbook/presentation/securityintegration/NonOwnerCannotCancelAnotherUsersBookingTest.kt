package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test

class NonOwnerCannotCancelAnotherUsersBookingTest : SecurityIntegrationFixture() {
    @Test
    fun `given another user's booking when a non-owner cancels it then it returns 403`() {
        val ownerToken = registerAndLogin(uniqueEmail())
        val otherToken = registerAndLogin(uniqueEmail())
        val bookableId = registerFlightWithOneSeat()

        val bookingResult =
            mockMvc.post("/bookings") {
                header("Authorization", "Bearer $ownerToken")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("bookableId" to bookableId))
            }.andReturn()
        val bookingId = objectMapper.readTree(bookingResult.response.contentAsString)["id"].asLong()

        mockMvc.post("/bookings/$bookingId/cancel") {
            header("Authorization", "Bearer $otherToken")
        }.andExpect { status { isForbidden() } }
    }
}
