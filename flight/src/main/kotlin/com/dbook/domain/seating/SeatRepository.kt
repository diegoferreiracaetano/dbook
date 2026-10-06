package com.dbook.domain.seating

/** Persistence port for individual [Seat]s belonging to a [Bookable]. */
interface SeatRepository {
    fun findById(id: Long): Seat?

    /** Several seats in one query, for a list. */
    fun findAllById(ids: Collection<Long>): List<Seat> = ids.mapNotNull { findById(it) }

    /** The full seat map for a [Bookable], e.g. for `GET /bookables/{id}/seats`. */
    fun findByBookableId(bookableId: Long): List<Seat>

    /** Backs [Bookable.availableCapacity], now derived from AVAILABLE seats instead of stored. */
    fun countAvailable(bookableId: Long): Int

    fun saveAll(seats: List<Seat>): List<Seat>

    /** Removes seats that were never booked (see `planSeatChange`). */
    fun deleteAll(seatIds: List<Long>)

    /** Atomically flips the seat to RESERVED under an optimistic lock; throws if it isn't AVAILABLE. */
    fun reserve(seatId: Long): Seat

    /** Atomically flips the seat back to AVAILABLE, e.g. after a booking is cancelled. */
    fun release(seatId: Long): Seat
}
