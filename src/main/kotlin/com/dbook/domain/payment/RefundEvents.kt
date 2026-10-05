package com.dbook.domain.payment

import com.dbook.domain.messaging.OutboxEvent
import java.time.Instant

object RefundEvents {
    const val COMPLETED = "refund.completed"

    fun completed(
        refund: Refund,
        customerId: Long,
        title: String,
        at: Instant,
    ) = OutboxEvent(
        "refund",
        requireNotNull(refund.id).toString(),
        COMPLETED,
        mapOf(
            "refundId" to refund.id,
            "bookingId" to refund.bookingId,
            "customerId" to customerId,
            "title" to title,
            "amount" to refund.amount.toPlainString(),
        ),
        at,
    )
}
