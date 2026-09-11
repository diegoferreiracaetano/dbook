package com.dbook.application.registerflightusecase

import com.dbook.application.RegisterFlightCommand
import com.dbook.application.RegisterFlightUseCase
import com.dbook.domain.Airport
import com.dbook.domain.AirportRepository
import com.dbook.domain.Flight
import com.dbook.domain.FlightRepository
import com.dbook.domain.Seat
import com.dbook.domain.SeatClass
import com.dbook.domain.SeatRepository
import com.dbook.domain.SeatStatus
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
        val withId = if (flight.id == null) copyWithId(flight, (saved.size + 1).toLong()) else flight
        saved += withId
        return withId
    }

    override fun search(
        originIataCode: String,
        destinationIataCode: String,
        date: LocalDate,
    ): List<Flight> = emptyList()

    override fun findActive(): List<Flight> = saved.filter { it.active }

    private fun copyWithId(
        flight: Flight,
        id: Long,
    ) = Flight(
        id = id,
        title = flight.title,
        price = flight.price,
        totalCapacity = flight.totalCapacity,
        availableCapacity = flight.availableCapacity,
        active = flight.active,
        flightNumber = flight.flightNumber,
        origin = flight.origin,
        destination = flight.destination,
        departureTime = flight.departureTime,
        arrivalTime = flight.arrivalTime,
        seatClass = flight.seatClass,
    )
}

class FakeSeatRepository : SeatRepository {
    val saved = mutableListOf<Seat>()

    override fun findById(id: Long): Seat? = saved.find { it.id == id }

    override fun findByBookableId(bookableId: Long): List<Seat> = saved.filter { it.bookableId == bookableId }

    override fun countAvailable(bookableId: Long): Int =
        saved.count { it.bookableId == bookableId && it.status == SeatStatus.AVAILABLE }

    override fun saveAll(seats: List<Seat>): List<Seat> {
        val withIds =
            seats.mapIndexed { index, seat ->
                Seat(saved.size + index + 1L, seat.bookableId, seat.label, seat.status)
            }
        saved += withIds
        return withIds
    }

    override fun reserve(seatId: Long): Seat = throw UnsupportedOperationException("not used by RegisterFlightUseCase")

    override fun release(seatId: Long): Seat = throw UnsupportedOperationException("not used by RegisterFlightUseCase")
}

// Shared "given": GRU and GIG exist as airports; every scenario below registers a flight
// between them (or a nonexistent code) using this fixed pair.
abstract class RegisterFlightUseCaseFixture {
    private val gru = Airport(id = 1, iataCode = "GRU", name = "Guarulhos", city = "São Paulo", country = "Brasil")
    private val gig = Airport(id = 2, iataCode = "GIG", name = "Galeão", city = "Rio de Janeiro", country = "Brasil")
    protected val flightRepository = FakeFlightRepository()
    protected val seatRepository = FakeSeatRepository()
    protected val useCase =
        RegisterFlightUseCase(flightRepository, FakeAirportRepository(listOf(gru, gig)), seatRepository)

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
