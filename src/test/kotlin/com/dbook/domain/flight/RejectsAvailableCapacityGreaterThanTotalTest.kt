package com.dbook.domain.flight

import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsAvailableCapacityGreaterThanTotalTest : FlightTestFixture() {
    @Test
    fun `given availableCapacity over totalCapacity when built then throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            buildFlight(totalCapacity = 100, availableCapacity = 101)
        }
    }
}
