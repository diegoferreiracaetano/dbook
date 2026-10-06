package com.dbook.presentation.payment

import com.dbook.application.payment.CancellationAction
import com.dbook.application.payment.CancellationBlocker
import com.dbook.application.payment.CancellationPolicy
import java.math.BigDecimal
import java.time.LocalDateTime

data class CancellationPolicyResponse(
    val bookingId: Long,
    val action: CancellationAction,
    val refundAmount: BigDecimal?,
    val refundableUntil: LocalDateTime?,
    val blockedBy: CancellationBlocker?,
) {
    companion object {
        fun from(policy: CancellationPolicy) =
            CancellationPolicyResponse(
                policy.bookingId,
                policy.action,
                policy.refundAmount,
                policy.refundableUntil,
                policy.blockedBy,
            )
    }
}
