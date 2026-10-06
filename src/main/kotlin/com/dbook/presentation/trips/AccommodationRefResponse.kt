package com.dbook.presentation.trips

import com.dbook.domain.accommodation.Accommodation

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
