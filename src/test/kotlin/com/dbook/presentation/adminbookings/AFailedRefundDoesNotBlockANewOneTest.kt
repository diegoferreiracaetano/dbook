package com.dbook.presentation.adminbookings

import kotlin.test.Test
import kotlin.test.assertEquals

class AFailedRefundDoesNotBlockANewOneTest : AdminBookingsFixture() {
    @Test
    fun `given a failed refund when refunding the booking again with another key then it completes`() {
        val (_, booking) = paidBooking()
        val support = staff()
        gateway.failure = "declined"
        try {
            refund(support, booking)
        } finally {
            gateway.failure = null
        }

        val again = refund(support, booking)

        assertEquals("COMPLETED", statusOfRefund(again))
    }
}
