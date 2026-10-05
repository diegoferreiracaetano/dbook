package com.dbook.application.payment.refund

import com.dbook.domain.payment.RefundWindowClosedException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InsideTheLastDayARefundNeedsAnOverrideTest : RefundUseCaseFixture() {
    @Test
    fun `given a flight leaving in 6 hours when refunding without override then the window is closed`() {
        assertFailsWith<RefundWindowClosedException> { committed { refundBooking.execute(command(bookingId = 101)) } }

        assertTrue(refunds.all().isEmpty())
        assertTrue(gateway.calls.isEmpty())
    }
}
