package com.dbook.application.payment.refund

import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.payment.RefundStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AGatewayFailureLeavesAFailedRefundAndTheBookingConfirmedTest : RefundUseCaseFixture() {
    @Test
    fun `given a gateway that refuses when refunding then the refund FAILED, the booking and the seat untouched`() {
        gateway.failure = "card declined by the processor"

        val refund = committed { refundBooking.execute(command()) }

        assertEquals(RefundStatus.FAILED, refund.status)
        assertEquals("card declined by the processor", refund.failureReason)
        assertEquals(BookingStatus.CONFIRMED, statusOf(100))
        assertTrue(seats.released.isEmpty())
        assertEquals(AuditAction.REFUND_FAILED, audit.events.last().action)
        assertEquals(1.0, counted("failed"))
    }
}
