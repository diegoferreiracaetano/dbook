package com.dbook.domain.booking

import com.dbook.domain.Booking
import com.dbook.domain.BookingStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class NewBookingStartsAsPendingTest : BookingTestFixture() {
    @Test
    fun `given a bookable flight when a booking is created then it starts as PENDING`() {
        val booking = Booking(bookable = flight, customerId = 1)

        assertEquals(BookingStatus.PENDING, booking.status)
    }
}
