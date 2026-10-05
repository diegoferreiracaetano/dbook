package com.dbook.domain.catalog.flight

import kotlin.test.Test
import kotlin.test.assertFailsWith

class AFlightArrivesAfterItDepartsAndGoesBetweenTwoAirportsTest {
    @Test
    fun `given an arrival before the departure or the same airport twice when building then it is refused`() {
        assertFailsWith<IllegalArgumentException> { aFlight(arrival = departure) }
        assertFailsWith<IllegalArgumentException> { aFlight(arrival = departure.minusHours(1)) }
        assertFailsWith<IllegalArgumentException> { aFlight(destination = gru) }
    }
}
