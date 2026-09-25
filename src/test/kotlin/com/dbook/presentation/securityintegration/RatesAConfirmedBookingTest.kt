package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test

class RatesAConfirmedBookingTest : SecurityIntegrationFixture() {
    @Test
    fun `given a CONFIRMED booking when reviewed then it returns 201 with the rating and comment`() {
        val token = registerAndLogin(uniqueEmail())
        val (bookableId, seatId) = registerFlightWithOneSeat()

        val bookingResult =
            mockMvc.post("/bookings") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("bookableId" to bookableId, "seatId" to seatId))
            }.andReturn()
        val bookingId = objectMapper.readTree(bookingResult.response.contentAsString)["id"].asLong()

        mockMvc.post("/payments") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    mapOf(
                        "bookingIds" to listOf(bookingId),
                        "cardLast4" to "4242",
                        "cardholderName" to "Jane Doe",
                    ),
                )
        }

        mockMvc.post("/reviews") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    mapOf("bookingId" to bookingId, "rating" to 5, "comment" to "Great flight!"),
                )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.bookingId") { value(bookingId) }
            jsonPath("$.rating") { value(5) }
            jsonPath("$.comment") { value("Great flight!") }
        }
    }
}
