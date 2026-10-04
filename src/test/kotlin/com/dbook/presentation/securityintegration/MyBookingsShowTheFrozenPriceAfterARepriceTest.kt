package com.dbook.presentation.securityintegration

import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

class MyBookingsShowTheFrozenPriceAfterARepriceTest : SecurityIntegrationFixture() {
    @Test
    fun `given a booking made at 100 when the flight is repriced then my bookings show 100 and the flight shows 150`() {
        val token = registerAndLogin(uniqueEmail())
        val (bookableId, seatId) = registerFlightWithOneSeat()
        book(token, bookableId, seatId)

        reprice(bookableId, "150.00")

        val mine =
            objectMapper.readTree(
                mockMvc.get("/v1/bookings") { header("Authorization", "Bearer $token") }
                    .andReturn().response.contentAsString,
            )
        assertEquals(100.0, mine[0]["price"].asDouble())
        assertEquals(150.0, mine[0]["flight"]["price"].asDouble())
    }
}
