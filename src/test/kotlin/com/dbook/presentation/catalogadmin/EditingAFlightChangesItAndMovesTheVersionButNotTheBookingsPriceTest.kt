package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals

class EditingAFlightChangesItAndMovesTheVersionButNotTheBookingsPriceTest : CatalogAdminFixture() {
    @Test
    fun `given a flight with a booking when its price changes then the booking keeps the price it was made at`() {
        val token = manager()
        val id = createFlight(token)
        val customer = registerAndLogin(uniqueEmail())
        val booking = book(customer, id, seatIdOf(id, "1A"))

        val result = edit(token, id, mapOf("price" to 180.0, "flightNumber" to "DB777"))

        assertEquals(200, result.response.status)
        assertEquals(180.0, json(result)["flight"]["price"].asDouble())
        assertEquals("DB777", json(result)["flight"]["flightNumber"].asText())
        assertEquals(1, json(result)["flight"]["version"].asInt())
        val mine = json(get(customer, "/v1/bookings"))
        assertEquals(100.0, mine.first { it["id"].asLong() == booking }["price"].asDouble())
    }
}
