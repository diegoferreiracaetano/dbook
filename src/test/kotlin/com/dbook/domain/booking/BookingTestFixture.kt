package com.dbook.domain.booking

import com.dbook.domain.Airport
import com.dbook.domain.Flight
import com.dbook.domain.SeatClass
import java.math.BigDecimal
import java.time.LocalDateTime

// Shared "given" for every Booking scenario below — one Bookable with available capacity.
abstract class BookingTestFixture {
    protected val flight =
        Flight(
            id = 1,
            title = "GRU-GIG",
            price = BigDecimal("500.00"),
            totalCapacity = 180,
            availableCapacity = 179,
            flightNumber = "DB1234",
            origin = Airport(id = 1, iataCode = "GRU", name = "Guarulhos", city = "São Paulo", country = "Brasil"),
            destination =
                Airport(id = 2, iataCode = "GIG", name = "Galeão", city = "Rio de Janeiro", country = "Brasil"),
            departureTime = LocalDateTime.of(2026, 10, 1, 8, 0),
            arrivalTime = LocalDateTime.of(2026, 10, 1, 9, 10),
            seatClass = SeatClass.ECONOMY,
        )
}
