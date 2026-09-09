package com.dbook.domain.flight

import com.dbook.domain.Airport
import com.dbook.domain.Flight
import com.dbook.domain.SeatClass
import java.math.BigDecimal
import java.time.LocalDateTime

abstract class FlightTestFixture {
    private val origin = Airport(id = 1, iataCode = "GRU", name = "Guarulhos", city = "São Paulo", country = "Brasil")
    private val destination =
        Airport(id = 2, iataCode = "GIG", name = "Galeão", city = "Rio de Janeiro", country = "Brasil")

    protected fun buildFlight(
        price: BigDecimal = BigDecimal("500.00"),
        totalCapacity: Int = 180,
        availableCapacity: Int = 180,
    ) = Flight(
        title = "GRU-GIG",
        price = price,
        totalCapacity = totalCapacity,
        availableCapacity = availableCapacity,
        flightNumber = "DB1234",
        origin = origin,
        destination = destination,
        departureTime = LocalDateTime.of(2026, 10, 1, 8, 0),
        arrivalTime = LocalDateTime.of(2026, 10, 1, 9, 10),
        seatClass = SeatClass.ECONOMY,
    )
}
