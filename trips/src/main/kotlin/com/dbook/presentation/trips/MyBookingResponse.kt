package com.dbook.presentation.trips

import com.dbook.application.trips.BookingWithDetails
import com.dbook.domain.accommodation.Accommodation
import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.flight.Flight
import com.dbook.presentation.booking.StayResponse
import com.dbook.presentation.flight.FlightResponse
import com.dbook.presentation.review.ReviewResponse
import com.dbook.presentation.seating.SeatResponse
import java.math.BigDecimal

data class MyBookingResponse(
    val id: Long?,
    val status: BookingStatus,
    val seat: SeatResponse?,
    val flight: FlightResponse?,
    val stay: StayResponse?,
    val accommodation: AccommodationRefResponse?,
    val review: ReviewResponse?,
    val price: BigDecimal,
    val discount: BigDecimal,
    val paidAmount: BigDecimal,
) {
    companion object {
        // Bookable is abstract; the only concrete specialization today is Flight (see the
        // same note on Mappers.kt) — this cast is the first presentation-layer place that
        // needs Flight-specific fields out of a Booking, not just its bookableId.
        fun from(details: BookingWithDetails) =
            MyBookingResponse(
                id = details.booking.id,
                status = details.booking.status,
                seat = details.seat?.let(SeatResponse::from),
                flight = (details.booking.bookable as? Flight)?.let(FlightResponse::from),
                stay = details.booking.stay?.let(StayResponse::from),
                accommodation = (details.booking.bookable as? Accommodation)?.let(AccommodationRefResponse::from),
                review = details.review?.let { ReviewResponse.from(it) },
                price = details.booking.price,
                discount = details.booking.discount,
                paidAmount = details.booking.paidAmount,
            )
    }
}
