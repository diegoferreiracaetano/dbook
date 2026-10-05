package com.dbook.presentation.performance

import com.dbook.QueryCounter
import com.dbook.presentation.accommodation.HotelApiFixture
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals

class TheHotelSearchCostsTheSameQueriesForOneHotelOrManyTest : HotelApiFixture() {
    @Autowired
    lateinit var queryCounter: QueryCounter

    @Test
    fun `given a destination when its hotels grow from one to four then the search costs the same queries`() {
        val team = manager()
        val destination = newDestination()
        createHotel(team, destination)
        found(destination, day(0), day(2))

        val withOne = queryCounter.queriesOf { search(destination, day(0), day(2)) }
        repeat(3) { createHotel(team, destination) }
        val withFour = queryCounter.queriesOf { search(destination, day(0), day(2)) }

        assertEquals(4, found(destination, day(0), day(2)).size)
        assertEquals(withOne, withFour, "the search asks something more for each hotel")
    }
}
