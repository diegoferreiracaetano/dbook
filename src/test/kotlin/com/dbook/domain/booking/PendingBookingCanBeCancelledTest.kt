package com.dbook.domain.booking

import com.dbook.domain.Booking
import com.dbook.domain.BookingStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class PendingBookingCanBeCancelledTest : BookingTestFixture() {
    @Test
    fun `given a PENDING booking when it is cancelled then it moves to CANCELLED`() {
        val booking = Booking(bookable = flight, customerId = 1)

        val cancelled = booking.cancel()

        assertEquals(BookingStatus.CANCELLED, cancelled.status)
    }
}
