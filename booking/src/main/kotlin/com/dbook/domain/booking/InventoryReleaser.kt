package com.dbook.domain.booking

/**
 * Gives back what one kind of booking holds when it ends: the seat of a flight, the nights of a stay. Each kind of
 * bookable (flight, hotel, and any that comes) implements it; booking does not know which kinds exist, it asks every
 * releaser whether the booking is its own.
 */
interface InventoryReleaser {
    fun supports(booking: Booking): Boolean

    /** Gives it back, inside the transaction of whatever ended the booking. */
    fun release(booking: Booking)

    /** Called after [release], in the same transaction: what to announce once it commits (nothing, by default). */
    fun announceAfterCommit(booking: Booking) = Unit
}
