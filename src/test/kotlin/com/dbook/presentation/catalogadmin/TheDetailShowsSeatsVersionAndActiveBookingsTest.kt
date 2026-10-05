package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals

class TheDetailShowsSeatsVersionAndActiveBookingsTest : CatalogAdminFixture() {
    @Test
    fun `given a flight with one pending booking when reading it then seats, layout, version and bookings are there`() {
        val token = manager()
        val id = createFlight(token, flightBody(capacity = 12))
        val customer = registerAndLogin(uniqueEmail())
        book(customer, id, seatIdOf(id, "1A"))

        val detail = json(flight(token, id))

        assertEquals(12, detail["flight"]["totalCapacity"].asInt())
        assertEquals(11, detail["flight"]["availableSeats"].asInt())
        assertEquals(1, detail["flight"]["reservedSeats"].asInt())
        assertEquals(listOf(3, 3), detail["flight"]["seatLayout"].map { it.asInt() })
        assertEquals("SCHEDULED", detail["flight"]["status"].asText())
        assertEquals(1, detail["activeBookings"].asInt())
        assertEquals(0, detail["flight"]["version"].asInt())
    }
}
