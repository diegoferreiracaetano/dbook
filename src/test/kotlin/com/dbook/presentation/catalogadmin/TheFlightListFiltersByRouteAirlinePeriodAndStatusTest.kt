package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals

class TheFlightListFiltersByRouteAirlinePeriodAndStatusTest : CatalogAdminFixture() {
    @Test
    fun `given flights of two routes when filtering then each filter finds the right ones, soonest first`() {
        val token = manager()
        val day = uniqueDeparture()
        val early = createFlight(token, flightBody(departure = day))
        val late = createFlight(token, flightBody(departure = day.plusHours(5)))
        val other = createFlight(token, flightBody(departure = day.plusHours(2), origin = "GIG", destination = "GRU"))
        cancelFlight(token, other)
        val window = arrayOf("departureFrom" to day.toString(), "departureTo" to day.plusHours(6).toString())

        fun ids(vararg extra: Pair<String, String>) =
            json(searchFlights(token, *window, *extra))["items"].map { it["id"].asLong() }

        assertEquals(listOf(early, other, late), ids())
        assertEquals(listOf(early, late), ids("origin" to "gru", "destination" to "gig"))
        assertEquals(listOf(other), ids("origin" to "GIG"))
        assertEquals(listOf(other), ids("status" to "CANCELLED"))
        assertEquals(listOf(early, late), ids("status" to "SCHEDULED", "airline" to "LA"))
        assertEquals(emptyList(), ids("airline" to "G3"))
    }
}
