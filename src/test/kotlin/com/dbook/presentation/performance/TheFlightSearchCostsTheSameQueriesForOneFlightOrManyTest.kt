package com.dbook.presentation.performance

import com.dbook.QueryCounter
import com.dbook.domain.catalog.FlightRepository
import com.dbook.presentation.catalogadmin.CatalogAdminFixture
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TheFlightSearchCostsTheSameQueriesForOneFlightOrManyTest : CatalogAdminFixture() {
    @Autowired
    lateinit var flights: FlightRepository

    @Autowired
    lateinit var queryCounter: QueryCounter

    private fun queriesToSearch(date: LocalDate) = queryCounter.queriesOf { flights.search("GRU", "GIG", date) }

    @Test
    fun `given a day when its flights grow from one to six then the public search costs the same queries`() {
        val team = manager()
        val departure = uniqueDeparture()
        createFlight(team, flightBody(departure))
        queriesToSearch(departure.toLocalDate())

        val withOne = queriesToSearch(departure.toLocalDate())
        repeat(5) { createFlight(team, flightBody(departure)) }
        val withSix = queriesToSearch(departure.toLocalDate())

        assertEquals(withOne, withSix, "the search asks something more for each flight")
    }
}
