package com.dbook.presentation.booking

import com.dbook.domain.accommodation.Accommodation
import com.dbook.domain.booking.Stay
import java.math.BigDecimal
import java.time.LocalDate

/** The room part of a booking: which room type, for which nights, for how many, at the rate the booking froze. */
data class StayResponse(
    val roomTypeId: Long,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val nights: Long,
    val guests: Int,
    val nightlyRate: BigDecimal,
) {
    companion object {
        fun from(stay: Stay) =
            StayResponse(stay.roomTypeId, stay.checkIn, stay.checkOut, stay.nights, stay.guests, stay.nightlyRate)
    }
}

/** The hotel a stay is at. */
data class AccommodationRefResponse(
    val id: Long?,
    val name: String,
    val city: String,
    val destinationIataCode: String,
) {
    companion object {
        fun from(hotel: Accommodation) =
            AccommodationRefResponse(hotel.id, hotel.name, hotel.destination.city, hotel.destination.iataCode)
    }
}
