package com.dbook.domain.seating.seatplan

import com.dbook.domain.seating.SeatMapTarget
import com.dbook.domain.seating.planSeatChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LessCapacityRemovesTheLastSeatsByPositionTest {
    @Test
    fun `given 12 seats when the capacity becomes 10 then the last two by position go, 2E and 2F`() {
        val change = planSeatChange(1, a320Seats(12), "Airbus A320", SeatMapTarget("Airbus A320", 10), emptySet())

        assertEquals(setOf("2E", "2F"), change.toRemove.map { it.label }.toSet())
        assertTrue(change.toAdd.isEmpty())
    }

    @Test
    fun `given 60 seats when the capacity becomes 58 then 10E and 10F go, row 10 sorts after row 9`() {
        val change = planSeatChange(1, a320Seats(60), "Airbus A320", SeatMapTarget("Airbus A320", 58), emptySet())

        assertEquals(setOf("10E", "10F"), change.toRemove.map { it.label }.toSet())
    }
}
