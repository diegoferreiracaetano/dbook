package com.dbook.domain.payment

// the amount and the outcome, never the free-text note (it may mention the customer)
fun Refund.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "bookingId" to bookingId,
        "paymentId" to paymentId,
        "amount" to amount.toPlainString(),
        "reason" to reason.name,
        "status" to status.name,
        "failureReason" to failureReason,
    )
