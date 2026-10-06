package com.dbook.domain.flight

import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsNegativeAvailableCapacityTest : FlightTestFixture() {
    @Test
    fun `given a negative availableCapacity when a Flight is built then it throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            buildFlight(availableCapacity = -1)
        }
    }
}
