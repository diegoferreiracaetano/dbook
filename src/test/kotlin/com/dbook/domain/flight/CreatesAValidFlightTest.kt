package com.dbook.domain.flight

import kotlin.test.Test
import kotlin.test.assertEquals

class CreatesAValidFlightTest : FlightTestFixture() {
    @Test
    fun `given valid flight attributes when a Flight is built then it has the given available capacity`() {
        val flight = buildFlight()

        assertEquals(180, flight.availableCapacity)
    }
}
