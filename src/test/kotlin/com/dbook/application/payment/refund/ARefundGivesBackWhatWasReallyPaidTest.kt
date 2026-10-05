package com.dbook.application.payment.refund

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class ARefundGivesBackWhatWasReallyPaidTest : RefundUseCaseFixture() {
    @Test
    fun `given a booking paid with a 40 discount when refunded then 460 goes back, not the 500 of its price`() {
        val refund = committed { refundBooking.execute(command(bookingId = 103)) }

        assertEquals(BigDecimal("460.00"), refund.amount)
        assertEquals(listOf(Triple(8L, BigDecimal("460.00"), "refund-1")), gateway.calls)
    }

    @Test
    fun `given a booking paid in full when refunded then its whole price goes back`() {
        val refund = committed { refundBooking.execute(command(bookingId = 100)) }

        assertEquals(BigDecimal("500.00"), refund.amount)
    }
}
