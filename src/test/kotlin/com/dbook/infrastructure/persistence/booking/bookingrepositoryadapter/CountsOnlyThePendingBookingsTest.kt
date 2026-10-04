package com.dbook.infrastructure.persistence.booking.bookingrepositoryadapter

import com.dbook.application.booking.bookingconcurrency.BookingConcurrencyFixture
import kotlin.test.Test
import kotlin.test.assertEquals

// Against the real PostgreSQL: the native count (the one the partial index serves) must count
// PENDING bookings and nothing else. Relative counts, because the database is shared by every
// integration test.
class CountsOnlyThePendingBookingsTest : BookingConcurrencyFixture() {
    @Test
    fun `given pending and paid bookings when counting then only the pending ones are counted`() {
        val before = bookingRepository.countPending()
        val userId = registerUser()
        val bookableId = registerFlight(totalCapacity = 2)
        val (first, second) = seatIdsOf(bookableId)
        val firstBooking = bookSeat(bookableId, first, userId)
        bookSeat(bookableId, second, userId)

        assertEquals(before + 2, bookingRepository.countPending())

        pay(firstBooking, userId)

        assertEquals(before + 1, bookingRepository.countPending())
    }
}
