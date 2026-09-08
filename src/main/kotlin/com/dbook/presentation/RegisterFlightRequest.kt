package com.dbook.presentation

import com.dbook.domain.SeatClass
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDateTime

data class RegisterFlightRequest(
    @get:Schema(example = "DB1234")
    val flightNumber: String,
    @get:Schema(example = "GRU", description = "IATA code, must exist in the airport table")
    val originIataCode: String,
    @get:Schema(example = "GIG", description = "IATA code, must exist in the airport table")
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
)
