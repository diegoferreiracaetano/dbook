package com.dbook.application.payment.refund

import com.dbook.domain.payment.RefundAlreadyRequestedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ASecondAttendantRefundingTheSameBookingIsAConflictTest : RefundUseCaseFixture() {
    @Test
    fun `given a refund in progress when another attendant refunds the same booking then it is a conflict`() {
        gateway.failure = null
        // the first one is left REQUESTED, as when the process died between the insert and the gateway
        refunds.save(
            com.dbook.domain.payment.Refund(
                paymentId = 7,
                bookingId = 100,
                amount = java.math.BigDecimal("500.00"),
                reason = com.dbook.domain.payment.RefundReason.OTHER,
                idempotencyKey = "first",
                requestFingerprint = "f",
                requestedBy = 4,
                createdAt = now,
            ),
        )

        assertFailsWith<RefundAlreadyRequestedException> {
            committed { refundBooking.execute(command(key = "second")) }
        }

        assertEquals(1, refunds.all().size)
        assertEquals(1.0, counted("conflict"))
    }
}
