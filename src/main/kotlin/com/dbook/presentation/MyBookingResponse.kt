package com.dbook.presentation

import com.dbook.application.BookingWithDetails
import com.dbook.domain.BookingStatus
import com.dbook.domain.Flight

data class MyBookingResponse(
    val id: Long?,
    val status: BookingStatus,
    val seat: SeatResponse,
    val flight: FlightResponse,
) {
    companion object {
        // Bookable is abstract; the only concrete specialization today is Flight (see the
        // same note on Mappers.kt) — this cast is the first presentation-layer place that
        // needs Flight-specific fields out of a Booking, not just its bookableId.
        fun from(details: BookingWithDetails) =
            MyBookingResponse(
                id = details.booking.id,
                status = details.booking.status,
                seat = SeatResponse.from(details.seat),
                flight = FlightResponse.from(details.booking.bookable as Flight),
            )
    }
}
