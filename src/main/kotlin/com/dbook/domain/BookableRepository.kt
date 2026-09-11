package com.dbook.domain

/**
 * Persistence port for [Bookable] itself — deliberately generic (not flight-specific),
 * since any concrete specialization (flights today, accommodations later) shares the
 * same availability-tracking behavior.
 */
interface BookableRepository {
    fun findById(id: Long): Bookable?

    /** Atomically decrements available capacity by one; the caller must have already checked availability. */
    fun decrementAvailability(bookableId: Long): Bookable

    /** Atomically increments available capacity by one, e.g. after a booking is cancelled. */
    fun incrementAvailability(bookableId: Long): Bookable
}
