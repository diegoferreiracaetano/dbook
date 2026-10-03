package com.dbook.application.bookingconcurrency

import com.dbook.domain.BookingStatus
import com.dbook.domain.Role
import com.dbook.domain.SeatStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

// Paying and cancelling read the same PENDING booking and each writes its own outcome.
// Without the booking-level optimistic lock (V24) both can commit, leaving a CONFIRMED
// booking whose seat was released — the same seat sold twice. Many rounds because a race
// only misbehaves some of the time.
class OnlyOneOfPayAndCancelWinsTest : BookingConcurrencyFixture() {
    @Test
    fun `given a pending booking when it is paid and cancelled at the same time then exactly one wins`() {
        val userId = registerUser()
        val bookableId = registerFlight(totalCapacity = ROUNDS)

        seatIdsOf(bookableId).forEach { seatId ->
            val bookingId = bookSeat(bookableId, seatId, userId)

            val wins =
                race(
                    { pay(bookingId, userId) },
                    { cancelBookingUseCase.execute(bookingId, userId, Role.CLIENT) },
                )

            assertEquals(1, wins, "exactly one of pay/cancel should win")
            assertSeatMatchesBooking(bookingId, seatId)
        }
    }

    private fun assertSeatMatchesBooking(
        bookingId: Long,
        seatId: Long,
    ) {
        val booking = requireNotNull(bookingRepository.findById(bookingId))
        val seat = requireNotNull(seatRepository.findById(seatId))
        assertNotEquals(BookingStatus.PENDING, booking.status)
        val expectedSeat = if (booking.status == BookingStatus.CONFIRMED) SeatStatus.RESERVED else SeatStatus.AVAILABLE
        assertEquals(expectedSeat, seat.status, "the seat must match the booking outcome")
    }

    private companion object {
        const val ROUNDS = 20
    }
}
