package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.Test

class ConfirmsPendingBookingsWhenPaidTest : SecurityIntegrationFixture() {
    @Test
    fun `given a PENDING booking when paid then it returns 201 and the booking becomes CONFIRMED`() {
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
        }.andExpect {
            status { isCreated() }
            jsonPath("$.amount") { value(100.00) }
            jsonPath("$.cardLast4") { value("4242") }
            jsonPath("$.bookingIds[0]") { value(bookingId) }
            jsonPath("$.status") { value("CONFIRMED") }
        }

        mockMvc.get("/bookings") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            jsonPath("$[0].status") { value("CONFIRMED") }
        }
    }
}
