package com.dbook.presentation.adminbookings

import kotlin.test.Test
import kotlin.test.assertEquals

class RefundingAPaidBookingCompletesItEndToEndTest : AdminBookingsFixture() {
    @Test
    fun `given a paid booking when support refunds it then it completes, the seat is free and the gateway was asked`() {
        val (customer, booking) = paidBooking()
        val support = staff()
        val before = gateway.calls.size

        val result = refund(support, booking)

        assertEquals(201, result.response.status)
        assertEquals("COMPLETED", statusOfRefund(result))
        assertEquals(100.0, bodyOf(result)["amount"].asDouble())
        val detail = bodyOf(adminBooking(support, booking))
        assertEquals("REFUNDED", detail["booking"]["status"].asText())
        assertEquals("COMPLETED", detail["refund"]["status"].asText())
        assertEquals("AVAILABLE", seatStatusOf(booking))
        assertEquals(before + 1, gateway.calls.size)
        assertEquals("refund-${refundIdOf(result)}", gateway.calls.last().idempotencyKey)
        assertEquals("REFUNDED", bodyOf(myBookings(customer))[0]["status"].asText())
    }
}
