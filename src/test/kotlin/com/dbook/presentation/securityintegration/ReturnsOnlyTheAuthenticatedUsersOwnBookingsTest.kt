package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.Test

class ReturnsOnlyTheAuthenticatedUsersOwnBookingsTest : SecurityIntegrationFixture() {
    @Test
    fun `given bookings from two different users when listing my bookings then only my own come back`() {
        val ownerToken = registerAndLogin(uniqueEmail())
        val otherToken = registerAndLogin(uniqueEmail())
        val (ownBookableId, ownSeatId) = registerFlightWithOneSeat()
        val (otherBookableId, otherSeatId) = registerFlightWithOneSeat()

        val bookingResult =
            mockMvc.post("/bookings") {
                header("Authorization", "Bearer $ownerToken")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("bookableId" to ownBookableId, "seatId" to ownSeatId))
            }.andReturn()
        val ownBookingId = objectMapper.readTree(bookingResult.response.contentAsString)["id"].asLong()

        mockMvc.post("/bookings") {
            header("Authorization", "Bearer $otherToken")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("bookableId" to otherBookableId, "seatId" to otherSeatId))
        }

        mockMvc.get("/bookings") {
            header("Authorization", "Bearer $ownerToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].id") { value(ownBookingId) }
            jsonPath("$[0].seat.id") { value(ownSeatId) }
            jsonPath("$[0].flight.id") { value(ownBookableId) }
            jsonPath("$[0].flight.aircraftType") { value("Airbus A320") }
        }
    }
}
