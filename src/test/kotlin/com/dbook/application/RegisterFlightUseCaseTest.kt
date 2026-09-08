package com.dbook.application

import com.dbook.domain.Airport
import com.dbook.domain.AirportNotFoundException
import com.dbook.domain.AirportRepository
import com.dbook.domain.Flight
import com.dbook.domain.FlightRepository
import com.dbook.domain.SeatClass
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class FakeAirportRepository(private val airports: List<Airport>) : AirportRepository {
    override fun findByIataCode(iataCode: String): Airport? = airports.find { it.iataCode == iataCode }
}

private class FakeFlightRepository : FlightRepository {
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
}

class RegisterFlightUseCaseTest {
    private val gru = Airport(id = 1, iataCode = "GRU", name = "Guarulhos", city = "São Paulo", country = "Brasil")
    private val gig = Airport(id = 2, iataCode = "GIG", name = "Galeão", city = "Rio de Janeiro", country = "Brasil")
    private val airportRepository = FakeAirportRepository(listOf(gru, gig))
    private val flightRepository = FakeFlightRepository()
    private val useCase = RegisterFlightUseCase(flightRepository, airportRepository)

    private fun command(
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

    @Test
    fun `registers a flight resolving airports by IATA code`() {
        val flight = useCase.execute(command())

        assertEquals("GRU", flight.origin.iataCode)
        assertEquals("GIG", flight.destination.iataCode)
        assertEquals(180, flight.availableCapacity)
        assertEquals(1, flightRepository.saved.size)
    }

    @Test
    fun `throws when origin airport does not exist`() {
        assertFailsWith<AirportNotFoundException> {
            useCase.execute(command(origin = "XXX"))
        }
    }

    @Test
    fun `throws when destination airport does not exist`() {
        assertFailsWith<AirportNotFoundException> {
            useCase.execute(command(destination = "YYY"))
        }
    }
}
