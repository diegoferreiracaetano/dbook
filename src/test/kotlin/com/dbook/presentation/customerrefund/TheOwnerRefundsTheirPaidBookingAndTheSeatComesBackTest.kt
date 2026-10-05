package com.dbook.presentation.customerrefund

import kotlin.test.Test
import kotlin.test.assertEquals

class TheOwnerRefundsTheirPaidBookingAndTheSeatComesBackTest : CustomerRefundFixture() {
    @Test
    fun `given a paid booking when the owner asks for a refund then it completes and the seat is free`() {
        val (token, booking) = paidBooking()
        val before = gateway.calls.size

        val result = refundRequest(token, booking)

        assertEquals(201, result.response.status)
        assertEquals("COMPLETED", bodyOf(result)["status"].asText())
        assertEquals(100.0, bodyOf(result)["amount"].asDouble())
        assertEquals("CUSTOMER_REQUEST", bodyOf(result)["reason"].asText())
        assertEquals(before + 1, gateway.calls.size)
        assertEquals("AVAILABLE", seatStatusOf(booking))
        assertEquals("ALREADY_REFUNDED", bodyOf(policyOf(token, booking))["blockedBy"].asText())
        assertEquals("REFUNDED", statusOfBookingInList(token, booking))
    }

    private fun statusOfBookingInList(
        token: String,
        booking: Long,
    ) = bodyOf(myBookings(token)).first { it["id"].asLong() == booking }["status"].asText()
}
