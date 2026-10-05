package com.dbook.application.payment

import com.dbook.application.common.fingerprintOf
import com.dbook.domain.identity.Actor
import com.dbook.domain.payment.RefundReason

data class RefundCommand(
    val actor: Actor,
    val bookingId: Long,
    val reason: RefundReason,
    val note: String?,
    val override: Boolean,
    val idempotencyKey: String,
) {
    fun fingerprint(): String = fingerprintOf(bookingId, reason, note?.trim(), override)
}
