package com.dbook.presentation

import com.dbook.application.FeaturedDestination
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class DestinationResponse(
    val iataCode: String,
    val city: String,
    val country: String,
    val photoUrl: String,
    val region: String,
    @get:JsonProperty("isPopular")
    val isPopular: Boolean,
    val lowestPrice: BigDecimal?,
) {
    companion object {
        fun from(destination: FeaturedDestination) =
            DestinationResponse(
                iataCode = destination.airport.iataCode,
                city = destination.airport.city,
                country = destination.airport.country,
                photoUrl = destination.airport.photoUrl,
                region = destination.airport.region,
                isPopular = destination.airport.isPopular,
                lowestPrice = destination.lowestPrice,
            )
    }
}
