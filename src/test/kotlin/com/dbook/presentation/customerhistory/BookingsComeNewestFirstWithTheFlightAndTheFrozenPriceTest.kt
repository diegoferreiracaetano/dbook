package com.dbook.presentation.customerhistory

import kotlin.test.Test
import kotlin.test.assertEquals

class BookingsComeNewestFirstWithTheFlightAndTheFrozenPriceTest : CustomerHistoryFixture() {
    @Test
    fun `given three bookings and a repriced flight when listing in pages of two then newest first, price frozen`() {
        val email = uniqueEmail()
        val token = registerAndLogin(email)
        val (firstFlight, firstSeat) = registerFlightWithOneSeat()
        val first = book(token, firstFlight, firstSeat)
        reprice(firstFlight, "150.00")
        val (second, third) = bookSeats(token, 2)
        val staffToken = supportToken()
        val customerId = userIdOf(email)

        val page0 = history(staffToken, customerId, "bookings", "size" to "2")
        val page1 = history(staffToken, customerId, "bookings", "size" to "2", "page" to "1")

        assertEquals(listOf(third, second), idsOf(page0))
        assertEquals(listOf(first), idsOf(page1))
        assertEquals(3, bodyOf(page0)["totalElements"].asInt())
        val oldest = bodyOf(page1)["items"][0]
        assertEquals(100.0, oldest["price"].asDouble())
        assertEquals("PENDING", oldest["status"].asText())
        assertEquals("GRU", oldest["origin"].asText())
        assertEquals("GIG", oldest["destination"].asText())
        assertEquals(true, oldest["flightNumber"].asText().isNotBlank())
    }
}
