package com.dbook.domain.seating.seatplan

import com.dbook.domain.seating.SeatMapTarget
import com.dbook.domain.seating.planSeatChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoreCapacityAddsSeatsAtTheEndTest {
    @Test
    fun `given 6 seats when the capacity becomes 9 then the 3 new ones continue the labels and nothing is removed`() {
        val change = planSeatChange(1, a320Seats(6), "Airbus A320", SeatMapTarget("Airbus A320", 9), emptySet())

        assertEquals(listOf("2A", "2B", "2C"), change.toAdd.map { it.label })
        assertTrue(change.toRemove.isEmpty())
    }
}
