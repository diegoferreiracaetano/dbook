package com.dbook.domain

/** Persistence port for individual [Seat]s belonging to a [Bookable]. */
interface SeatRepository {
    fun findById(id: Long): Seat?

    /** The full seat map for a [Bookable], e.g. for `GET /bookables/{id}/seats`. */
    fun findByBookableId(bookableId: Long): List<Seat>

    /** Backs [Bookable.availableCapacity], now derived from AVAILABLE seats instead of stored. */
    fun countAvailable(bookableId: Long): Int

    fun saveAll(seats: List<Seat>): List<Seat>

    /** Atomically flips the seat to RESERVED under an optimistic lock; throws if it isn't AVAILABLE. */
    fun reserve(seatId: Long): Seat

    /** Atomically flips the seat back to AVAILABLE, e.g. after a booking is cancelled. */
    fun release(seatId: Long): Seat
}
