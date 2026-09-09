package com.dbook.domain.booking

import com.dbook.domain.Booking
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CancelledBookingCannotBeCancelledAgainTest : BookingTestFixture() {
    @Test
    fun `given a CANCELLED booking when cancelling it again then it throws IllegalStateException`() {
        val cancelled = Booking(bookable = flight, customerId = 1).cancel()

        assertFailsWith<IllegalStateException> { cancelled.cancel() }
    }
}
