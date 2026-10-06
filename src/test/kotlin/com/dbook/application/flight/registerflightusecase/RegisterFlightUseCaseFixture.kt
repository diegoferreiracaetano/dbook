package com.dbook.application.flight.registerflightusecase

import com.dbook.application.audit.FakeAuditLog
import com.dbook.application.catalog.AirportLookup
import com.dbook.application.common.RecordingOutboxWriter
import com.dbook.application.flight.AirlineLookup
import com.dbook.application.flight.RegisterFlightCommand
import com.dbook.application.flight.RegisterFlightUseCase
import com.dbook.application.pricing.FlightPriceRecorder
import com.dbook.application.pricing.InMemoryPriceHistory
import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.AirportRepository
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.access.Role
import com.dbook.domain.flight.Airline
import com.dbook.domain.flight.AirlineRepository
import com.dbook.domain.flight.Flight
import com.dbook.domain.flight.FlightRepository
import com.dbook.domain.flight.SeatClass
import com.dbook.domain.seating.Seat
import com.dbook.domain.seating.SeatRepository
import com.dbook.domain.seating.SeatStatus
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class FakeAirportRepository(private val airports: List<Airport>) : AirportRepository {
    override fun findByIataCode(iataCode: String): Airport? = airports.find { it.iataCode == iataCode }

    override fun findAll(): List<Airport> = airports

    override fun findById(id: Long): Airport? = error("not needed for this test")

    override fun save(airport: Airport): Airport = error("not needed for this test")

    override fun delete(id: Long) = error("not needed for this test")

    override fun flightCount(id: Long): Long = error("not needed for this test")
}

class FakeAirlineRepository(private val airlines: List<Airline>) : AirlineRepository {
    override fun findByIataCode(iataCode: String): Airline? = airlines.find { it.iataCode == iataCode }

    override fun findById(id: Long): Airline? = error("not needed for this test")

    override fun findAll(): List<Airline> = error("not needed for this test")

    override fun save(airline: Airline): Airline = error("not needed for this test")

    override fun delete(id: Long) = error("not needed for this test")

    override fun flightCount(id: Long): Long = error("not needed for this test")
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

    override fun findLowestPrice(
        destinationIataCode: String,
        from: LocalDate,
        to: LocalDate,
    ): BigDecimal? = error("not used by RegisterFlightUseCase")

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
        airline = flight.airline,
        origin = flight.origin,
        destination = flight.destination,
        departureTime = flight.departureTime,
        arrivalTime = flight.arrivalTime,
        seatClass = flight.seatClass,
        aircraftType = flight.aircraftType,
    )

    override fun update(
        flight: Flight,
        expectedVersion: Long?,
    ): Flight = error("not needed for this test")
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

    override fun deleteAll(seatIds: List<Long>) = error("not needed for this test")
}

// Shared "given": GRU and GIG exist as airports; every scenario below registers a flight
// between them (or a nonexistent code) using this fixed pair.
abstract class RegisterFlightUseCaseFixture {
    private val latam = Airline(id = 1, iataCode = "LA", name = "LATAM Airlines")
    private val gru =
        Airport(
            id = 1,
            iataCode = "GRU",
            name = "Guarulhos",
            city = "São Paulo",
            country = "Brasil",
            photoUrl = "https://example.com/photo.jpg",
            region = "América do Sul",
            isPopular = false,
        )
    private val gig =
        Airport(
            id = 2,
            iataCode = "GIG",
            name = "Galeão",
            city = "Rio de Janeiro",
            country = "Brasil",
            photoUrl = "https://example.com/photo.jpg",
            region = "América do Sul",
            isPopular = false,
        )
    protected val flightRepository = FakeFlightRepository()
    protected val seatRepository = FakeSeatRepository()
    protected val auditLog = FakeAuditLog()
    protected val outbox = RecordingOutboxWriter()
    protected val priceHistory = InMemoryPriceHistory()
    protected val admin = Actor(id = 1, role = Role.SUPER_ADMIN)
    protected val useCase =
        RegisterFlightUseCase(
            flightRepository,
            AirlineLookup(FakeAirlineRepository(listOf(latam))),
            AirportLookup(FakeAirportRepository(listOf(gru, gig))),
            seatRepository,
            auditLog,
            FlightPriceRecorder(
                priceHistory,
                outbox,
                Clock.fixed(Instant.parse("2026-10-04T12:00:00Z"), ZoneOffset.UTC),
            ),
        )

    protected fun command(
        origin: String = "GRU",
        destination: String = "GIG",
        airline: String = "LA",
        aircraftType: String = "Airbus A320",
        totalCapacity: Int = 180,
    ) = RegisterFlightCommand(
        actor = admin,
        flightNumber = "DB1234",
        airlineIataCode = airline,
        originIataCode = origin,
        destinationIataCode = destination,
        departureTime = LocalDateTime.of(2026, 10, 1, 8, 0),
        arrivalTime = LocalDateTime.of(2026, 10, 1, 9, 10),
        seatClass = SeatClass.ECONOMY,
        price = BigDecimal("500.00"),
        totalCapacity = totalCapacity,
        aircraftType = aircraftType,
    )
}
