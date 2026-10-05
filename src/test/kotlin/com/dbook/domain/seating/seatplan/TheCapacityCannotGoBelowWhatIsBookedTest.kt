package com.dbook.domain.seating.seatplan

import com.dbook.domain.seating.FlightSeatConflictException
import com.dbook.domain.seating.SeatMapTarget
import com.dbook.domain.seating.planSeatChange
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TheCapacityCannotGoBelowWhatIsBookedTest {
    @Test
    fun `given reserved seats when the capacity goes below their number then it is refused`() {
        val seats = a320Seats(12, reserved = setOf("1A", "1B", "1C"))

        assertFailsWith<FlightSeatConflictException> {
            planSeatChange(1, seats, "Airbus A320", SeatMapTarget("Airbus A320", 2), emptySet())
        }
    }

    @Test
    fun `given a reserved seat among the last ones when shrinking then it is refused`() {
        val seats = a320Seats(12, reserved = setOf("2F"))

        assertFailsWith<FlightSeatConflictException> {
            planSeatChange(1, seats, "Airbus A320", SeatMapTarget("Airbus A320", 10), emptySet())
        }
    }

    @Test
    fun `given a free seat with booking history among the last ones when shrinking then it is refused`() {
        val seats = a320Seats(12)
        val withHistory = setOf(seats.last().id!!)

        assertFailsWith<FlightSeatConflictException> {
            planSeatChange(1, seats, "Airbus A320", SeatMapTarget("Airbus A320", 10), withHistory)
        }
    }
}
