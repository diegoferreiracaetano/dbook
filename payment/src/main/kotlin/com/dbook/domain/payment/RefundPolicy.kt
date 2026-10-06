package com.dbook.domain.payment

import java.time.Duration
import java.time.LocalDateTime

private const val FULL_REFUND_HOURS = 24L

/** Full refund until 24 hours before departure; after that only a SUPER_ADMIN, explicitly and with a note. */
object RefundPolicy {
    val FULL_REFUND_WINDOW: Duration = Duration.ofHours(FULL_REFUND_HOURS)

    /** True inside the last 24 hours before [departure] or after it. A booking with no departure has no window. */
    fun needsOverride(
        departure: LocalDateTime?,
        now: LocalDateTime,
    ): Boolean = departure != null && now.isAfter(departure.minus(FULL_REFUND_WINDOW))
}
