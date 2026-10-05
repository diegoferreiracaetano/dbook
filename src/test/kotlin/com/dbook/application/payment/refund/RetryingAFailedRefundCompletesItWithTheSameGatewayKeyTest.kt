package com.dbook.application.payment.refund

import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.payment.RefundStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class RetryingAFailedRefundCompletesItWithTheSameGatewayKeyTest : RefundUseCaseFixture() {
    @Test
    fun `given a failed refund when the gateway recovers and it is retried then it completes with the same key`() {
        gateway.failure = "timeout"
        val failed = committed { refundBooking.execute(command()) }
        gateway.failure = null

        val retried = committed { retryRefund.execute(requireNotNull(failed.id), support) }

        assertEquals(RefundStatus.COMPLETED, retried.status)
        assertEquals(BookingStatus.REFUNDED, statusOf(100))
        assertEquals(listOf("refund-1", "refund-1"), gateway.calls.map { it.third })
        assertEquals(1, refunds.all().size)
    }
}
