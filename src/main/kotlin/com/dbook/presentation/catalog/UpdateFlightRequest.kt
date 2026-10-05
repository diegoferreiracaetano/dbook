package com.dbook.presentation.catalog

import com.dbook.application.catalog.UpdateFlightCommand
import com.dbook.domain.catalog.SeatClass
import com.dbook.domain.identity.Actor
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

data class UpdateFlightRequest(
    @get:Schema(example = "3", description = "the version the editor read: if the flight changed since, it is a 409")
    val version: Long,
    @get:Schema(example = "DB1234")
    val flightNumber: String,
    @get:Schema(example = "LA")
    val airlineIataCode: String,
    @get:Schema(example = "GRU")
    val originIataCode: String,
    @get:Schema(example = "GIG")
    val destinationIataCode: String,
    @get:Schema(type = "string", example = "2026-10-01T08:00:00")
    val departureTime: LocalDateTime,
    @get:Schema(type = "string", example = "2026-10-01T09:10:00")
    val arrivalTime: LocalDateTime,
    @get:Schema(example = "ECONOMY")
    val seatClass: SeatClass,
    @get:Schema(example = "450.00")
    val price: BigDecimal,
    @get:Schema(example = "180")
    val totalCapacity: Int,
    @get:Schema(example = "Airbus A320")
    val aircraftType: String,
) {
    fun toCommand(
        actor: Actor,
        flightId: Long,
    ) = UpdateFlightCommand(
        actor, flightId, version, flightNumber, airlineIataCode, originIataCode, destinationIataCode, departureTime,
        arrivalTime, seatClass, price, totalCapacity, aircraftType,
    )
}
