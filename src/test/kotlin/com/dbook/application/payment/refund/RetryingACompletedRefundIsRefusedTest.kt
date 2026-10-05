package com.dbook.application.payment.refund

import com.dbook.domain.payment.RefundNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RetryingACompletedRefundIsRefusedTest : RefundUseCaseFixture() {
    @Test
    fun `given a completed refund when retrying then it is refused and the gateway is not called again`() {
        val done = committed { refundBooking.execute(command()) }

        assertFailsWith<IllegalStateException> { retryRefund.execute(requireNotNull(done.id), support) }
        assertFailsWith<RefundNotFoundException> { retryRefund.execute(99, support) }

        assertEquals(1, gateway.calls.size)
    }
}
