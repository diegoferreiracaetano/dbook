package com.dbook.domain.booking

import com.dbook.domain.Booking
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ConfirmedBookingCannotBeConfirmedAgainTest : BookingTestFixture() {
    @Test
    fun `given a CONFIRMED booking when confirming it again then it throws IllegalStateException`() {
        val confirmed = Booking(bookable = flight, customerId = 1).confirm()

        assertFailsWith<IllegalStateException> { confirmed.confirm() }
    }
}
