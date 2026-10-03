package com.dbook.application.bookingconcurrency

import com.dbook.application.RegisterBookingCommand
import kotlin.test.Test
import kotlin.test.assertEquals

// Exercises the seat-level optimistic lock (item 10.6, replacing the bookable-level lock
// from item 2.2) against a real Postgres, provisioned by Testcontainers (item 4.2).
class OnlyOneOfTwoBookingsForTheSameSeatWinsTest : BookingConcurrencyFixture() {
    @Test
    fun `given a flight with one seat when two bookings race for it then only one succeeds`() {
        val userId = registerUser()
        val bookableId = registerFlight(totalCapacity = 1)
        val seatId = seatIdsOf(bookableId).single()

        val successes =
            race(
                { registerBookingUseCase.execute(RegisterBookingCommand(bookableId, seatId, userId)) },
                { registerBookingUseCase.execute(RegisterBookingCommand(bookableId, seatId, userId)) },
            )

        assertEquals(1, successes, "exactly one booking should succeed; the other fails with a version conflict")
    }
}
