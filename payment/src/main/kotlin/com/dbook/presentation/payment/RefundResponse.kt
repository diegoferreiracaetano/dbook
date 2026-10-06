package com.dbook.presentation.payment

import com.dbook.domain.payment.Refund
import com.dbook.domain.payment.RefundReason
import com.dbook.domain.payment.RefundStatus
import java.math.BigDecimal
import java.time.Instant

/** `status` says how it ended: COMPLETED, or FAILED (the money did not move and it can be retried), or REQUESTED. */
data class RefundResponse(
    val id: Long?,
    val bookingId: Long,
    val paymentId: Long,
    val amount: BigDecimal,
    val reason: RefundReason,
    val status: RefundStatus,
    val failureReason: String?,
    val requestedBy: Long,
    val createdAt: Instant,
    val completedAt: Instant?,
) {
    companion object {
        fun from(refund: Refund) =
            RefundResponse(
                id = refund.id,
                bookingId = refund.bookingId,
                paymentId = refund.paymentId,
                amount = refund.amount,
                reason = refund.reason,
                status = refund.status,
                failureReason = refund.failureReason,
                requestedBy = refund.requestedBy,
                createdAt = refund.createdAt,
                completedAt = refund.completedAt,
            )
    }
}
