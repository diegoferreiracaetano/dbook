package com.dbook.presentation.favorite

import com.dbook.domain.favorite.FavoriteDestination
import com.dbook.domain.favorite.FavoriteFlight
import com.dbook.domain.favorite.FavoriteType
import com.dbook.domain.favorite.FavoriteView
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

data class FavoriteDestinationResponse(
    val iataCode: String,
    val name: String,
    val city: String,
    val country: String,
    val photoUrl: String,
) {
    companion object {
        fun from(destination: FavoriteDestination) =
            FavoriteDestinationResponse(
                destination.iataCode,
                destination.name,
                destination.city,
                destination.country,
                destination.photoUrl,
            )
    }
}

data class FavoriteFlightResponse(
    val id: Long,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: LocalDateTime,
    val price: BigDecimal,
    val onSale: Boolean,
) {
    companion object {
        fun from(flight: FavoriteFlight) =
            FavoriteFlightResponse(
                flight.id,
                flight.flightNumber,
                flight.origin,
                flight.destination,
                flight.departureTime,
                flight.price,
                flight.onSale,
            )
    }
}

/** Exactly one of `destination` and `flight` is set, by `type`. */
data class FavoriteResponse(
    val type: FavoriteType,
    val id: String,
    val createdAt: Instant,
    val destination: FavoriteDestinationResponse?,
    val flight: FavoriteFlightResponse?,
) {
    companion object {
        fun from(view: FavoriteView) =
            FavoriteResponse(
                view.type,
                view.targetId,
                view.createdAt,
                view.destination?.let(FavoriteDestinationResponse::from),
                view.flight?.let(FavoriteFlightResponse::from),
            )
    }
}
