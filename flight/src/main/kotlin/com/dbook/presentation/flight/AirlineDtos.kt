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
