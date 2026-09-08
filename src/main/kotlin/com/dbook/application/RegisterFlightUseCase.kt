package com.dbook.application

import com.dbook.domain.AirportNotFoundException
import com.dbook.domain.AirportRepository
import com.dbook.domain.Flight
import com.dbook.domain.FlightRepository
import com.dbook.domain.SeatClass
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime

data class RegisterFlightCommand(
	val flightNumber: String,
	val originIataCode: String,
	val destinationIataCode: String,
	val departureTime: LocalDateTime,
	val arrivalTime: LocalDateTime,
	val seatClass: SeatClass,
	val price: BigDecimal,
	val totalCapacity: Int,
)

@Service
class RegisterFlightUseCase(
	private val flightRepository: FlightRepository,
	private val airportRepository: AirportRepository,
) {
	fun execute(command: RegisterFlightCommand): Flight {
		val origin = airportRepository.findByIataCode(command.originIataCode)
			?: throw AirportNotFoundException(command.originIataCode)
		val destination = airportRepository.findByIataCode(command.destinationIataCode)
			?: throw AirportNotFoundException(command.destinationIataCode)

		val flight = Flight(
			title = "${command.flightNumber} ${origin.iataCode}-${destination.iataCode}",
			price = command.price,
			totalCapacity = command.totalCapacity,
			availableCapacity = command.totalCapacity,
			flightNumber = command.flightNumber,
			origin = origin,
			destination = destination,
			departureTime = command.departureTime,
			arrivalTime = command.arrivalTime,
			seatClass = command.seatClass,
		)
		return flightRepository.save(flight)
	}
}
