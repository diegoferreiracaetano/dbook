package com.dbook.presentation

import com.dbook.domain.SeatClass
import java.math.BigDecimal
import java.time.LocalDateTime

data class RegisterFlightRequest(
	val flightNumber: String,
	val originIataCode: String,
	val destinationIataCode: String,
	val departureTime: LocalDateTime,
	val arrivalTime: LocalDateTime,
	val seatClass: SeatClass,
	val price: BigDecimal,
	val totalCapacity: Int,
)
