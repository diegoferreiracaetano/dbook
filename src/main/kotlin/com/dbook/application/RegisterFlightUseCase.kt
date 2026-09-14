package com.dbook.application

import com.dbook.domain.Airline
import com.dbook.domain.AirlineNotFoundException
import com.dbook.domain.AirlineRepository
import com.dbook.domain.Airport
import com.dbook.domain.AirportNotFoundException
import com.dbook.domain.AirportRepository
import com.dbook.domain.Flight
import com.dbook.domain.FlightRepository
import com.dbook.domain.Seat
import com.dbook.domain.SeatClass
import com.dbook.domain.SeatRepository
import com.dbook.domain.seatLayoutFor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

data class RegisterFlightCommand(
    val flightNumber: String,
    val airlineIataCode: String,
    val originIataCode: String,
    val destinationIataCode: String,
    val departureTime: LocalDateTime,
    val arrivalTime: LocalDateTime,
    val seatClass: SeatClass,
    val price: BigDecimal,
    val totalCapacity: Int,
    val aircraftType: String,
)

private val ROW_LETTERS = ('A'..'Z').toList()

/**
 * Registers a new [Flight], resolving origin/destination by IATA code, and generates its
 * seat map — seats per row and column letters come from [seatLayoutFor], based on
 * [RegisterFlightCommand.aircraftType], so different aircraft really do get different
 * layouts (2+2, 3+3, 3+4+3...). Requires ADMIN.
 */
@Service
class RegisterFlightUseCase(
    private val flightRepository: FlightRepository,
    private val airlineRepository: AirlineRepository,
    private val airportRepository: AirportRepository,
    private val seatRepository: SeatRepository,
) {
    @Transactional
    fun execute(command: RegisterFlightCommand): Flight {
        val airline = resolveAirline(command.airlineIataCode)
        val origin = resolveAirport(command.originIataCode)
        val destination = resolveAirport(command.destinationIataCode)

        val flight =
            Flight(
                title = "${command.flightNumber} ${origin.iataCode}-${destination.iataCode}",
                price = command.price,
                totalCapacity = command.totalCapacity,
                availableCapacity = command.totalCapacity,
                flightNumber = command.flightNumber,
                airline = airline,
                origin = origin,
                destination = destination,
                departureTime = command.departureTime,
                arrivalTime = command.arrivalTime,
                seatClass = command.seatClass,
                aircraftType = command.aircraftType,
            )
        val saved = flightRepository.save(flight)
        val bookableId = requireNotNull(saved.id) { "A saved Flight must have an id" }
        seatRepository.saveAll(generateSeatMap(bookableId, command.totalCapacity, command.aircraftType))

        // availableCapacity is derived from the seats just generated above, not from `saved`
        return requireNotNull(flightRepository.findById(bookableId)) {
            "Flight $bookableId was just saved but could not be reloaded"
        }
    }

    private fun resolveAirline(iataCode: String): Airline =
        airlineRepository.findByIataCode(iataCode) ?: throw AirlineNotFoundException(iataCode)

    private fun resolveAirport(iataCode: String): Airport =
        airportRepository.findByIataCode(iataCode) ?: throw AirportNotFoundException(iataCode)

    private fun generateSeatMap(
        bookableId: Long,
        totalCapacity: Int,
        aircraftType: String,
    ): List<Seat> {
        val seatsPerRow = seatLayoutFor(aircraftType).sum()
        return (0 until totalCapacity).map { index ->
            val row = index / seatsPerRow + 1
            val letter = ROW_LETTERS[index % seatsPerRow]
            Seat(bookableId = bookableId, label = "$row$letter")
        }
    }
}
