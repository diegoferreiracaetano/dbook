package com.dbook.domain.booking

import com.dbook.domain.Booking
import com.dbook.domain.BookingStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class PendingBookingCanBeConfirmedTest : BookingTestFixture() {
    @Test
    fun `given a PENDING booking when it is confirmed then it moves to CONFIRMED and records the payment`() {
        val booking = Booking(bookable = flight, seatId = seatId, customerId = 1)

        val confirmed = booking.confirm(paymentId = 42)

        assertEquals(BookingStatus.CONFIRMED, confirmed.status)
        assertEquals(42, confirmed.paymentId)
    }
}
