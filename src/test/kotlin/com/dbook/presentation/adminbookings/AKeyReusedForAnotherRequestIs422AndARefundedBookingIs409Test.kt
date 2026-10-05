package com.dbook.presentation.adminbookings

import kotlin.test.Test
import kotlin.test.assertEquals

class AKeyReusedForAnotherRequestIs422AndARefundedBookingIs409Test : AdminBookingsFixture() {
    @Test
    fun `given a used key on another booking and a booking refunded twice then 422 and 409`() {
        val (_, first) = paidBooking()
        val (_, second) = paidBooking()
        val support = staff()
        refund(support, first, key = "reused-key")

        assertEquals(422, refund(support, second, key = "reused-key").response.status)
        assertEquals(409, refund(support, first).response.status)
    }
}
