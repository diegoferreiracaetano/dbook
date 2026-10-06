package com.dbook.domain.payment

import java.math.BigDecimal
import java.time.Instant

enum class RefundReason { CUSTOMER_REQUEST, FLIGHT_CANCELLED, DUPLICATE, OTHER }

enum class RefundStatus { REQUESTED, COMPLETED, FAILED }

private const val MAX_NOTE_LENGTH = 500
private const val MAX_FAILURE_LENGTH = 255
private const val MAX_IDEMPOTENCY_KEY_LENGTH = 64

/**
 * Money going back to the customer for one [com.dbook.domain.booking.Booking]. It is born REQUESTED and persisted
 * before the payment gateway is called, then ends COMPLETED or FAILED; a FAILED one can be reopened to try again
 * (see [reopen]). Only one refund per booking can be alive at a time: a FAILED one does not count.
 */
class Refund(
    val id: Long? = null,
    val paymentId: Long,
    val bookingId: Long,
    val amount: BigDecimal,
    val reason: RefundReason,
    val note: String? = null,
    val status: RefundStatus = RefundStatus.REQUESTED,
    val idempotencyKey: String,
    val requestFingerprint: String,
    val requestedBy: Long,
    val failureReason: String? = null,
    val createdAt: Instant,
    val completedAt: Instant? = null,
    val version: Long = 0,
) {
    init {
        require(amount > BigDecimal.ZERO) { "amount must be positive" }
        require(note == null || note.length <= MAX_NOTE_LENGTH) { "note must have at most $MAX_NOTE_LENGTH characters" }
        require(idempotencyKey.isNotBlank() && idempotencyKey.length <= MAX_IDEMPOTENCY_KEY_LENGTH) {
            "idempotencyKey must have 1 to $MAX_IDEMPOTENCY_KEY_LENGTH characters"
        }
    }

    fun complete(now: Instant): Refund {
        check(status == RefundStatus.REQUESTED) { "Only a REQUESTED refund can be completed" }
        return copy(status = RefundStatus.COMPLETED, failureReason = null, completedAt = now)
    }

    fun fail(why: String): Refund {
        check(status == RefundStatus.REQUESTED) { "Only a REQUESTED refund can fail" }
        return copy(status = RefundStatus.FAILED, failureReason = why.take(MAX_FAILURE_LENGTH))
    }

    fun reopen(): Refund {
        check(status == RefundStatus.FAILED) { "Only a FAILED refund can be reopened" }
        return copy(status = RefundStatus.REQUESTED, failureReason = null)
    }

    private fun copy(
        status: RefundStatus,
        failureReason: String?,
        completedAt: Instant? = this.completedAt,
    ) = Refund(
        id, paymentId, bookingId, amount, reason, note, status, idempotencyKey, requestFingerprint, requestedBy,
        failureReason, createdAt, completedAt, version,
    )
}
