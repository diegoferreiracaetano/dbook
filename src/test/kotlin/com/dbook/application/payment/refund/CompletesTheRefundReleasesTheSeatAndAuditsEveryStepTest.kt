package com.dbook.application.payment.refund

import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.payment.RefundStatus
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class CompletesTheRefundReleasesTheSeatAndAuditsEveryStepTest : RefundUseCaseFixture() {
    @Test
    fun `given a confirmed booking when refunding then it is COMPLETED, the booking REFUNDED and the seat released`() {
        val refund = committed { refundBooking.execute(command()) }

        assertEquals(RefundStatus.COMPLETED, refund.status)
        assertEquals(BookingStatus.REFUNDED, statusOf(100))
        assertEquals(listOf(1000L), seats.released)
        assertEquals(listOf(100L to 1), broadcaster.broadcasts)
        assertEquals(listOf(Triple(7L, BigDecimal("500.00"), "refund-1")), gateway.calls)
        assertEquals(
            listOf(AuditAction.REFUND_REQUESTED, AuditAction.REFUND_COMPLETED),
            audit.events.map { it.action },
        )
        assertEquals(1.0, counted("completed"))
    }
}
