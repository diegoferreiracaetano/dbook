package com.dbook.infrastructure.cache

import kotlin.test.Test
import kotlin.test.assertEquals

class ARepeatedSearchIsAnsweredWithoutTheDatabaseTest : FlightSearchCacheFixture() {
    @Test
    fun `given a search was made when it is made again then it costs no query and counts a hit`() {
        val team = manager()
        val departure = uniqueDeparture()
        val id = createFlight(team, flightBody(departure))
        val first = searchOn(departure)
        val hitsBefore = hits()

        var second = emptyList<com.dbook.domain.flight.Flight>()
        val queries = queryCounter.queriesOf { second = searchOn(departure) }

        assertEquals(listOf(id), first.map { it.id })
        assertEquals(first.map { it.flightNumber to it.price }, second.map { it.flightNumber to it.price })
        assertEquals(0, queries)
        assertEquals(hitsBefore + 1, hits())
    }
}
