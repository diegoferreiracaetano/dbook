package com.dbook.application.payment.refund

import com.dbook.domain.payment.RefundStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class TheSameKeyTwiceIsOneRefundTest : RefundUseCaseFixture() {
    @Test
    fun `given the same key and request twice when refunding then it is one refund, one gateway call, one replay`() {
        val first = committed { refundBooking.execute(command()) }
        val second = committed { refundBooking.execute(command()) }

        assertEquals(first.id, second.id)
        assertEquals(RefundStatus.COMPLETED, second.status)
        assertEquals(1, gateway.calls.size)
        assertEquals(1, refunds.all().size)
        assertEquals(1.0, counted("replayed"))
    }
}
