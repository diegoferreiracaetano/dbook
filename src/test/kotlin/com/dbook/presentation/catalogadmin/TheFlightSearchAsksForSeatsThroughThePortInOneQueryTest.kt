package com.dbook.presentation.catalogadmin

import com.dbook.QueryCounter
import com.dbook.domain.flight.FlightRepository
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals

// M45 moved the "how many seats are free" question to the SeatAvailability port, at one query per flight (7 queries
// for 3 flights, measured then). M47 found that cost growing with every flight in the answer (the N+1) and asked for
// the whole list at once: the flights with their airline and airports in one query, the free seats in another.
class TheFlightSearchAsksForSeatsThroughThePortInOneQueryTest : CatalogAdminFixture() {
    @Autowired
    lateinit var flights: FlightRepository

    @Autowired
    lateinit var queryCounter: QueryCounter

    @Test
    fun `given three flights on a day when searched then it costs two queries however many flights`() {
        val team = manager()
        val departure = uniqueDeparture()
        repeat(3) { createFlight(team, flightBody(departure)) }

        var found = 0
        val queries = queryCounter.queriesOf { found = flights.search("GRU", "GIG", departure.toLocalDate()).size }

        assertEquals(3, found)
        assertEquals(2, queries)
    }
}
