package com.dbook.presentation.securityintegration

import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertEquals

// The cancel response is built from what the repository's save() returns, a path no read-back test covers.
class CancellingAfterARepriceReturnsTheFrozenPriceTest : SecurityIntegrationFixture() {
    @Test
    fun `given a booking made at 100 when the flight is repriced and the booking cancelled then it still says 100`() {
        val token = registerAndLogin(uniqueEmail())
        val (bookableId, seatId) = registerFlightWithOneSeat()
        val bookingId = book(token, bookableId, seatId)
        reprice(bookableId, "150.00")

        val cancelled =
            mockMvc.post(
                "/v1/bookings/$bookingId/cancel",
            ) { header("Authorization", "Bearer $token") }.andReturn()

        assertEquals(200, cancelled.response.status)
        assertEquals(100.0, objectMapper.readTree(cancelled.response.contentAsString)["price"].asDouble())
    }
}
