package com.dbook.domain.seating.seatplan

import com.dbook.domain.seating.SeatMapTarget
import com.dbook.domain.seating.planSeatChange
import kotlin.test.Test
import kotlin.test.assertTrue

class AnUnknownAircraftHasTheSameLayoutAsTheDefaultTest {
    @Test
    fun `given two aircraft names with the same layout when planning then it is not a layout change`() {
        val change = planSeatChange(1, a320Seats(12), "Airbus A320", SeatMapTarget("Some Other Jet", 12), setOf(100L))

        assertTrue(change.toAdd.isEmpty() && change.toRemove.isEmpty())
    }
}
