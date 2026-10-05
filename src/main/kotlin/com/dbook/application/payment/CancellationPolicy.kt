package com.dbook.application.payment

import java.math.BigDecimal
import java.time.LocalDateTime

/** What the customer can do about a booking they want out of. */
enum class CancellationAction {
    /** The booking is not paid: it is cancelled (`POST /bookings/{id}/cancel`), nothing to refund. */
    CANCEL,

    /** It is paid and still inside the refund window: ask for the money back (`POST /bookings/{id}/refund-request`). */
    REFUND_REQUEST,

    /** Nothing to do from the app; [CancellationPolicy.blockedBy] says why. */
    NONE,
}

enum class CancellationBlocker { WINDOW_CLOSED, REFUND_IN_PROGRESS, ALREADY_REFUNDED, ALREADY_CANCELLED }

/**
 * What the app shows **before** the customer confirms: whether they can get out, how much they would get back and
 * until when. [refundAmount] is what was actually paid for the booking (after any discount).
 */
data class CancellationPolicy(
    val bookingId: Long,
    val action: CancellationAction,
    val refundAmount: BigDecimal?,
    val refundableUntil: LocalDateTime?,
    val blockedBy: CancellationBlocker?,
)
