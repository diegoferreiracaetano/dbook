package com.dbook.presentation.promo

import kotlin.test.Test
import kotlin.test.assertEquals

class ARefundGivesBackWhatWasPaidAfterTheDiscountTest : PromoApiFixture() {
    @Test
    fun `given a booking paid with a 10 percent code when refunded then 90 goes back and the code is not given back`() {
        val (id, code) = newPromo(manager())
        val (token, booking) = customerWithABooking()
        payWith(token, listOf(booking), code)

        val refund = refund(staff(), booking)

        assertEquals(201, refund.response.status)
        assertEquals(90.0, bodyOf(refund)["amount"].asDouble())
        assertEquals(1, redeemed(id))
    }
}
