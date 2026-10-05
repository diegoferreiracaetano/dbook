package com.dbook.presentation.customerrefund

import kotlin.test.Test
import kotlin.test.assertEquals

class TheOwnersRefundFollowsTheStaffsRulesTest : CustomerRefundFixture() {
    @Test
    fun `given a booking inside the last day when the owner asks then it is a 409 and no money moves`() {
        val (token, booking) = paidBookingDepartingIn(6)
        val before = gateway.calls.size

        val result = refundRequest(token, booking)

        assertEquals(409, result.response.status)
        assertEquals("REFUND_WINDOW_CLOSED", bodyOf(result)["code"].asText())
        assertEquals(before, gateway.calls.size)
        assertEquals(0, refundRowsOf(booking))
    }

    @Test
    fun `given someone else's booking, an unpaid one, or no key when asking then 403, 409 and 400`() {
        val (_, booking) = paidBooking()
        val (stranger, _) = paidBooking()
        val pending = registerAndLogin(uniqueEmail())
        val (unpaid) = bookSeats(pending, 1)

        assertEquals(403, refundRequest(stranger, booking).response.status)
        assertEquals(409, refundRequest(pending, unpaid).response.status)
        assertEquals(400, refundRequest(pending, unpaid, key = null).response.status)
        assertEquals(0, refundRowsOf(booking))
    }

    @Test
    fun `given a booking already refunded when asking again with a new key then it is a 409`() {
        val (token, booking) = paidBooking()
        refundRequest(token, booking)

        assertEquals(409, refundRequest(token, booking).response.status)
        assertEquals(1, refundRowsOf(booking))
    }

    @Test
    fun `given a gateway that refuses when the owner asks then the refund is FAILED and a new key tries again`() {
        val (token, booking) = paidBooking()
        gateway.failure = "gateway refuses"

        val failed = refundRequest(token, booking)
        gateway.failure = null
        val again = refundRequest(token, booking)

        assertEquals("FAILED", bodyOf(failed)["status"].asText())
        assertEquals("COMPLETED", bodyOf(again)["status"].asText())
    }
}
