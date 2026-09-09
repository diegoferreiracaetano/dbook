package com.dbook.application.registerflightusecase

import com.dbook.application.RegisterFlightCommand
import com.dbook.application.RegisterFlightUseCase
import com.dbook.domain.Airport
import com.dbook.domain.AirportRepository
import com.dbook.domain.Flight
import com.dbook.domain.FlightRepository
import com.dbook.domain.SeatClass
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class FakeAirportRepository(private val airports: List<Airport>) : AirportRepository {
    override fun findByIataCode(iataCode: String): Airport? = airports.find { it.iataCode == iataCode }
}

class FakeFlightRepository : FlightRepository {
    val saved = mutableListOf<Flight>()

    override fun findById(id: Long): Flight? = saved.find { it.id == id }

    override fun save(flight: Flight): Flight {
        saved += flight
        return flight
    }

    override fun search(
        originIataCode: String,
        destinationIataCode: String,
        date: LocalDate,
    ): List<Flight> = emptyList()

    override fun findActive(): List<Flight> = saved.filter { it.active }
}

// Shared "given": GRU and GIG exist as airports; every scenario below registers a flight
// between them (or a nonexistent code) using this fixed pair.
abstract class RegisterFlightUseCaseFixture {
    private val gru = Airport(id = 1, iataCode = "GRU", name = "Guarulhos", city = "São Paulo", country = "Brasil")
    private val gig = Airport(id = 2, iataCode = "GIG", name = "Galeão", city = "Rio de Janeiro", country = "Brasil")
    protected val flightRepository = FakeFlightRepository()
    protected val useCase = RegisterFlightUseCase(flightRepository, FakeAirportRepository(listOf(gru, gig)))

    protected fun command(
        origin: String = "GRU",
        destination: String = "GIG",
    ) = RegisterFlightCommand(
        flightNumber = "DB1234",
        originIataCode = origin,
        destinationIataCode = destination,
        departureTime = LocalDateTime.of(2026, 10, 1, 8, 0),
        arrivalTime = LocalDateTime.of(2026, 10, 1, 9, 10),
        seatClass = SeatClass.ECONOMY,
        price = BigDecimal("500.00"),
        totalCapacity = 180,
    )
}
