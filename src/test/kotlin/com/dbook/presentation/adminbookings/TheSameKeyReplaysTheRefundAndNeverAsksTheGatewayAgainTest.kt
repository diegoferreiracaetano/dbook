package com.dbook.presentation.adminbookings

import kotlin.test.Test
import kotlin.test.assertEquals

class TheSameKeyReplaysTheRefundAndNeverAsksTheGatewayAgainTest : AdminBookingsFixture() {
    @Test
    fun `given the same key and request twice when refunding then it is one refund and one gateway call`() {
        val (_, booking) = paidBooking()
        val support = staff()
        val before = gateway.calls.size

        val first = refund(support, booking, key = "same-key-1")
        val second = refund(support, booking, key = "same-key-1")

        assertEquals(refundIdOf(first), refundIdOf(second))
        assertEquals("COMPLETED", statusOfRefund(second))
        assertEquals(before + 1, gateway.calls.size)
    }
}
