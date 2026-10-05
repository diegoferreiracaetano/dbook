package com.dbook.presentation.performance

import com.dbook.QueryCounter
import com.dbook.presentation.catalogadmin.CatalogAdminFixture
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals

class TheAdminFlightListCostsTheSameQueriesForAnyPageSizeTest : CatalogAdminFixture() {
    @Autowired
    lateinit var queryCounter: QueryCounter

    @Test
    fun `given enough flights when a page of one and a page of five are listed then the cost is the same`() {
        val team = manager()
        repeat(5) { createFlight(team) }
        searchFlights(team, "size" to "1")

        val one = queryCounter.queriesOf { searchFlights(team, "size" to "1") }
        val five = queryCounter.queriesOf { searchFlights(team, "size" to "5") }

        assertEquals(one, five, "the list asks something more for each flight")
    }
}
