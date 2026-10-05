package com.dbook.presentation.catalogadmin

import kotlin.test.Test
import kotlin.test.assertEquals

class TheFlightListIsPagedWithTheTotalTest : CatalogAdminFixture() {
    @Test
    fun `given three flights when listing in pages of two then the pages and the total are right`() {
        val token = manager()
        val day = uniqueDeparture()
        val ids = (0..2).map { createFlight(token, flightBody(departure = day.plusHours(it.toLong()))) }
        val window =
            arrayOf("departureFrom" to day.toString(), "departureTo" to day.plusHours(3).toString(), "size" to "2")

        val first = json(searchFlights(token, *window))
        val second = json(searchFlights(token, *window, "page" to "1"))

        assertEquals(ids.take(2), first["items"].map { it["id"].asLong() })
        assertEquals(ids.drop(2), second["items"].map { it["id"].asLong() })
        assertEquals(3, first["totalElements"].asInt())
    }
}
