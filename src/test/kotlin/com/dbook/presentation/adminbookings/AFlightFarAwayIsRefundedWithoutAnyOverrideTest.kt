package com.dbook.presentation.adminbookings

import kotlin.test.Test
import kotlin.test.assertEquals

class AFlightFarAwayIsRefundedWithoutAnyOverrideTest : AdminBookingsFixture() {
    @Test
    fun `given a flight 3 days away when support refunds then no override is needed`() {
        val (_, booking) = paidBookingDepartingIn(hours = 72)

        assertEquals("COMPLETED", statusOfRefund(refund(staff(), booking)))
    }
}
