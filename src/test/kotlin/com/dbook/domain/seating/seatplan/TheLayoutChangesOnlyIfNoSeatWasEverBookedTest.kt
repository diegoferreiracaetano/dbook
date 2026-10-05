package com.dbook.domain.seating.seatplan

import com.dbook.domain.seating.FlightSeatConflictException
import com.dbook.domain.seating.SeatMapTarget
import com.dbook.domain.seating.planSeatChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TheLayoutChangesOnlyIfNoSeatWasEverBookedTest {
    @Test
    fun `given a different layout and no bookings when planning then the whole map is rebuilt for the new layout`() {
        val current = a320Seats(12)

        val change = planSeatChange(1, current, "Airbus A320", SeatMapTarget("Embraer E195", 8), emptySet())

        assertEquals(current, change.toRemove)
        assertEquals(listOf("1A", "1B", "1C", "1D", "2A", "2B", "2C", "2D"), change.toAdd.map { it.label })
    }

    @Test
    fun `given a different layout and a seat that was booked when planning then it is refused`() {
        assertFailsWith<FlightSeatConflictException> {
            planSeatChange(1, a320Seats(12), "Airbus A320", SeatMapTarget("Embraer E195", 8), setOf(100L))
        }
    }
}
