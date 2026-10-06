package com.dbook.domain.seating

/**
 * How many seats of a bookable are free. A flight's `availableCapacity` is derived from this, so the catalog asks
 * `seating` through this port instead of reaching into its tables: the two concepts stay apart in the persistence too.
 */
interface SeatAvailability {
    fun availableSeats(bookableId: Long): Int

    /** The same for many bookables in one query, for a list: a bookable with no free seat is simply not in the map. */
    fun availableSeatsOf(bookableIds: Collection<Long>): Map<Long, Int>
}
