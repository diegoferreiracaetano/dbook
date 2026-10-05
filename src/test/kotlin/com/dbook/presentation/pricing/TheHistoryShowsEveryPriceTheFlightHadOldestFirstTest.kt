package com.dbook.presentation.pricing

import kotlin.test.Test
import kotlin.test.assertEquals

class TheHistoryShowsEveryPriceTheFlightHadOldestFirstTest : PricingApiFixture() {
    @Test
    fun `given a flight whose price changed twice when anyone reads the history then it has three points in order`() {
        val token = manager()
        val flight = createFlight(token)
        edit(token, flight, mapOf("price" to 80.0))
        edit(token, flight, mapOf("price" to 150.0))

        val result = history(flight)

        assertEquals(200, result.response.status)
        val json = body(result)
        assertEquals(listOf(100.0, 80.0, 150.0), json["points"].map { it["price"].asDouble() })
        assertEquals(150.0, json["current"].asDouble())
        assertEquals(80.0, json["lowest"].asDouble())
        assertEquals(150.0, json["highest"].asDouble())
    }

    @Test
    fun `given an edit that keeps the price when read then the history has no new point`() {
        val token = manager()
        val flight = createFlight(token)

        edit(token, flight, mapOf("flightNumber" to "DB4242"))

        assertEquals(1, body(history(flight))["points"].size())
    }

    @Test
    fun `given an unknown flight when reading the history then it is a 404`() {
        assertEquals(404, history(999_999_999).response.status)
    }

    @Test
    fun `given a price change when the edit is made then the event for the alerts is written with it`() {
        val token = manager()
        val flight = createFlight(token)

        edit(token, flight, mapOf("price" to 80.0))

        val events = outboxEvents("flight.price-changed", flight.toString())
        assertEquals(listOf(null, "100.00"), events.map { it["previousPrice"].takeIf { n -> !n.isNull }?.asText() })
        assertEquals("80.00", events.last()["price"].asText())
    }
}
