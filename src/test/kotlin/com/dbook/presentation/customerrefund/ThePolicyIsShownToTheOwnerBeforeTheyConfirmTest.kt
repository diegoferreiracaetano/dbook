package com.dbook.presentation.customerrefund

import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

class ThePolicyIsShownToTheOwnerBeforeTheyConfirmTest : CustomerRefundFixture() {
    @Test
    fun `given a paid booking far from departure when the owner asks then a refund is offered for what was paid`() {
        val (token, booking) = paidBooking()

        val result = policyOf(token, booking)

        assertEquals(200, result.response.status)
        assertEquals("REFUND_REQUEST", bodyOf(result)["action"].asText())
        assertEquals(100.0, bodyOf(result)["refundAmount"].asDouble())
        assertEquals(false, bodyOf(result)["refundableUntil"].isNull)
    }

    @Test
    fun `given a booking inside the last day when the owner asks then the window is closed`() {
        val (token, booking) = paidBookingDepartingIn(6)

        assertEquals("NONE", bodyOf(policyOf(token, booking))["action"].asText())
        assertEquals("WINDOW_CLOSED", bodyOf(policyOf(token, booking))["blockedBy"].asText())
    }

    @Test
    fun `given an unpaid booking when the owner asks then it can be cancelled`() {
        val token = registerAndLogin(uniqueEmail())
        val (booking) = bookSeats(token, 1)

        assertEquals("CANCEL", bodyOf(policyOf(token, booking))["action"].asText())
    }

    @Test
    fun `given someone else's booking, no token or none such when asking then 403, 401 and 404`() {
        val (_, booking) = paidBooking()
        val (stranger, _) = paidBooking()

        assertEquals(403, policyOf(stranger, booking).response.status)
        assertEquals(401, mockMvc.get("/v1/bookings/$booking/cancellation-policy").andReturn().response.status)
        assertEquals(404, policyOf(stranger, 999_999_999).response.status)
    }
}
