package com.dbook.presentation

import com.dbook.application.RegisterFlightCommand
import com.dbook.application.RegisterFlightUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/flights")
class FlightAdminController(
	private val registerFlightUseCase: RegisterFlightUseCase,
) {
	@PostMapping
	fun register(@RequestBody request: RegisterFlightRequest): ResponseEntity<FlightResponse> {
		val flight = registerFlightUseCase.execute(
			RegisterFlightCommand(
				flightNumber = request.flightNumber,
				originIataCode = request.originIataCode,
				destinationIataCode = request.destinationIataCode,
				departureTime = request.departureTime,
				arrivalTime = request.arrivalTime,
				seatClass = request.seatClass,
				price = request.price,
				totalCapacity = request.totalCapacity,
			),
		)
		return ResponseEntity.status(HttpStatus.CREATED).body(FlightResponse.from(flight))
	}
}
