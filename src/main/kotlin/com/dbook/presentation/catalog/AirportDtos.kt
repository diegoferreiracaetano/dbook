package com.dbook.presentation.catalog

import com.dbook.application.catalog.AirportCommand
import com.dbook.domain.catalog.Airport
import com.dbook.domain.identity.Actor
import io.swagger.v3.oas.annotations.media.Schema

data class AirportRequest(
    @get:Schema(example = "GRU", description = "3 capital letters; unique")
    val iataCode: String,
    @get:Schema(example = "Guarulhos")
    val name: String,
    @get:Schema(example = "São Paulo")
    val city: String,
    @get:Schema(example = "Brasil")
    val country: String,
    @get:Schema(example = "https://example.com/gru.jpg")
    val photoUrl: String,
    @get:Schema(example = "América do Sul")
    val region: String,
    @get:Schema(example = "true", description = "shows in the featured destinations")
    val isPopular: Boolean = false,
) {
    fun toCommand(actor: Actor) = AirportCommand(actor, iataCode, name, city, country, photoUrl, region, isPopular)
}

data class AirportResponse(
    val id: Long?,
    val iataCode: String,
    val name: String,
    val city: String,
    val country: String,
    val photoUrl: String,
    val region: String,
    val isPopular: Boolean,
) {
    companion object {
        fun from(airport: Airport) =
            AirportResponse(
                airport.id,
                airport.iataCode,
                airport.name,
                airport.city,
                airport.country,
                airport.photoUrl,
                airport.region,
                airport.isPopular,
            )
    }
}
