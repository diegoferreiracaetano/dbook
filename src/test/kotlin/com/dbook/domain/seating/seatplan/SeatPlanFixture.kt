package com.dbook.domain.seating.seatplan

import com.dbook.domain.seating.Seat
import com.dbook.domain.seating.SeatStatus
import com.dbook.domain.seating.generateSeats

// The seats of a 3+3 aircraft (A320), numbered with ids from 100, optionally with some already reserved.
fun a320Seats(
    capacity: Int,
    reserved: Set<String> = emptySet(),
): List<Seat> =
    generateSeats(1, "Airbus A320", 0, capacity).mapIndexed { i, seat ->
        Seat(
            100L + i,
            seat.bookableId,
            seat.label,
            if (seat.label in reserved) SeatStatus.RESERVED else SeatStatus.AVAILABLE,
        )
    }
