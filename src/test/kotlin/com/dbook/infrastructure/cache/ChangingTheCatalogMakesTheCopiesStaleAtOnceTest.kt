package com.dbook.infrastructure.cache

import kotlin.test.Test
import kotlin.test.assertEquals

class ChangingTheCatalogMakesTheCopiesStaleAtOnceTest : FlightSearchCacheFixture() {
    @Test
    fun `given a cached search when a flight is added then the next search sees it`() {
        val team = manager()
        val departure = uniqueDeparture()
        val first = createFlight(team, flightBody(departure))
        assertEquals(listOf(first), searchOn(departure).map { it.id })

        val second = createFlight(team, flightBody(departure))

        assertEquals(setOf(first, second), searchOn(departure).map { it.id }.toSet())
    }

    @Test
    fun `given a cached search when the flight is edited then the next search shows the new price`() {
        val team = manager()
        val departure = uniqueDeparture()
        val id = createFlight(team, flightBody(departure))
        searchOn(departure)

        edit(team, id, mapOf("price" to 150.00))

        assertEquals(150.0, searchOn(departure).single().price.toDouble())
    }

    @Test
    fun `given a cached search when the flight is cancelled then the next search no longer shows it`() {
        val team = manager()
        val departure = uniqueDeparture()
        val id = createFlight(team, flightBody(departure))
        assertEquals(listOf(id), searchOn(departure).map { it.id })

        cancelFlight(team, id)

        assertEquals(emptyList(), searchOn(departure).map { it.id })
    }
}
