package com.dbook.domain.booking

import com.dbook.domain.Booking
import com.dbook.domain.BookingStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class PendingBookingCanBeConfirmedTest : BookingTestFixture() {
    @Test
    fun `given a PENDING booking when it is confirmed then it moves to CONFIRMED`() {
        val booking = Booking(bookable = flight, customerId = 1)

        val confirmed = booking.confirm()

        assertEquals(BookingStatus.CONFIRMED, confirmed.status)
    }
}
