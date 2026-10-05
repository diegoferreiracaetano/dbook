package com.dbook.presentation.favorite

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ThePageShowsWhatTheFavoritePointsToTest : FavoriteApiFixture() {
    @Test
    fun `given a destination and a flight when listed then each carries its own details`() {
        val (token, _) = aCustomer()
        val (flight) = registerFlightWithOneSeat("GIG")
        save(token, "DESTINATION", "GRU")
        save(token, "FLIGHT", "$flight")

        val items = body(list(token))["items"]

        val flightItem = items[0]
        assertEquals("FLIGHT", flightItem["type"].asText())
        assertEquals("GRU", flightItem["flight"]["origin"].asText())
        assertEquals("GIG", flightItem["flight"]["destination"].asText())
        assertEquals(100.0, flightItem["flight"]["price"].asDouble())
        assertEquals(true, flightItem["flight"]["onSale"].asBoolean())
        assertEquals(true, flightItem["destination"].isNull)
        val destinationItem = items[1]
        assertEquals("GRU", destinationItem["destination"]["iataCode"].asText())
        assertEquals("São Paulo", destinationItem["destination"]["city"].asText())
        assertNull(destinationItem["flight"].takeIf { !it.isNull })
    }

    @Test
    fun `given a favorite flight when it is taken off sale then it stays in the list as not on sale`() {
        val (token, _) = aCustomer()
        val (flight) = registerFlightWithOneSeat()
        save(token, "FLIGHT", "$flight")

        jdbcTemplate.update("UPDATE bookable SET active = false WHERE id = ?", flight)

        assertEquals(false, body(list(token))["items"][0]["flight"]["onSale"].asBoolean())
    }
}
