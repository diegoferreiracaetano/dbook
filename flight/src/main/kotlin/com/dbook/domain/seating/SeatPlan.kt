package com.dbook.domain.seating

class FlightSeatConflictException(message: String) : RuntimeException(message)

data class SeatChange(
    val toAdd: List<Seat>,
    val toRemove: List<Seat>,
)

/** What the seat map should become: how many seats of which aircraft. */
data class SeatMapTarget(
    val aircraftType: String,
    val capacity: Int,
)

/**
 * What to do with the seat map when a flight's capacity or aircraft changes, or why it cannot be done.
 *
 * - A different **layout** (seats per row) rebuilds the whole map, and only if nobody ever booked a seat of this flight
 *   (a seat with booking history, even a cancelled one, is referenced and cannot go).
 * - More capacity adds seats at the end.
 * - Less capacity removes seats from the end, and only if they are free and have no history.
 */
fun planSeatChange(
    bookableId: Long,
    current: List<Seat>,
    oldAircraft: String,
    target: SeatMapTarget,
    seatsWithBookings: Set<Long>,
): SeatChange =
    when {
        seatLayoutFor(oldAircraft) != seatLayoutFor(target.aircraftType) ->
            rebuild(bookableId, current, target, seatsWithBookings)
        target.capacity >= current.size ->
            SeatChange(generateSeats(bookableId, target.aircraftType, current.size, target.capacity), emptyList())
        else -> SeatChange(emptyList(), seatsToRemove(current, oldAircraft, target.capacity, seatsWithBookings))
    }

private fun rebuild(
    bookableId: Long,
    current: List<Seat>,
    target: SeatMapTarget,
    seatsWithBookings: Set<Long>,
): SeatChange {
    if (seatsWithBookings.isNotEmpty()) {
        throw FlightSeatConflictException("The aircraft layout cannot change: seats of this flight were booked")
    }
    return SeatChange(generateSeats(bookableId, target.aircraftType, 0, target.capacity), current)
}

private fun seatsToRemove(
    current: List<Seat>,
    aircraft: String,
    newCapacity: Int,
    seatsWithBookings: Set<Long>,
): List<Seat> {
    val reserved = current.count { it.status == SeatStatus.RESERVED }
    if (newCapacity < reserved) {
        throw FlightSeatConflictException("The capacity cannot go below the $reserved seats already reserved")
    }
    val last = current.sortedBy { seatPosition(it.label, aircraft) }.takeLast(current.size - newCapacity)
    last.firstOrNull { it.status == SeatStatus.RESERVED || it.id in seatsWithBookings }?.let {
        throw FlightSeatConflictException("Seat ${it.label} has bookings: the capacity cannot go below its position")
    }
    return last
}
