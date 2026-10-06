package com.dbook.application.booking

import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.InventoryReleaser
import org.springframework.stereotype.Service

/**
 * Gives back what a booking was holding when it ends (cancelled, expired or refunded), inside the transaction of
 * whatever ended it. It does not know what a booking holds: each kind of bookable registers an [InventoryReleaser] and
 * the one that supports the booking gives it back.
 */
@Service
class BookingInventoryReleaser(
    private val releasers: List<InventoryReleaser>,
) {
    fun release(booking: Booking) {
        releasers.filter { it.supports(booking) }.forEach { it.release(booking) }
    }

    /** Call after [release], in the same transaction: lets the kind announce what changed once it commits. */
    fun announceAfterCommit(booking: Booking) {
        releasers.filter { it.supports(booking) }.forEach { it.announceAfterCommit(booking) }
    }
}
