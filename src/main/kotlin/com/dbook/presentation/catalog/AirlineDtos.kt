package com.dbook.presentation.catalog

import com.dbook.application.catalog.AirlineCommand
import com.dbook.domain.catalog.Airline
import com.dbook.domain.identity.Actor
import io.swagger.v3.oas.annotations.media.Schema

data class AirlineRequest(
    @get:Schema(example = "LA", description = "2 capital letters or digits; unique")
    val iataCode: String,
    @get:Schema(example = "LATAM Airlines")
    val name: String,
) {
    fun toCommand(actor: Actor) = AirlineCommand(actor, iataCode, name)
}

data class AirlineResponse(
    val id: Long?,
    val iataCode: String,
    val name: String,
) {
    companion object {
        fun from(airline: Airline) = AirlineResponse(airline.id, airline.iataCode, airline.name)
    }
}
