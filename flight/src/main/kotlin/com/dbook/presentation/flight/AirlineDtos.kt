package com.dbook.presentation.flight

import com.dbook.application.flight.AirlineCommand
import com.dbook.domain.common.access.Actor
import com.dbook.domain.flight.Airline
import io.swagger.v3.oas.annotations.media.Schema

data class AirlineRequest(
    @get:Schema(example = "LA", description = "2 capital letters or digits; unique")
    val iataCode: String,
    @get:Schema(example = "LATAM Airlines")
    val name: String,
    @get:Schema(example = "https://cdn.example.com/logos/LA.png", description = "https URL of the logo; optional")
    val logoUrl: String? = null,
) {
    fun toCommand(actor: Actor) = AirlineCommand(actor, iataCode, name, logoUrl)
}

data class AirlineResponse(
    val id: Long?,
    val iataCode: String,
    val name: String,
    val logoUrl: String?,
) {
    companion object {
        fun from(airline: Airline) = AirlineResponse(airline.id, airline.iataCode, airline.name, airline.logoUrl)
    }
}
