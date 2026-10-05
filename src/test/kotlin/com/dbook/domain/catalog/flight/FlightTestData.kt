package com.dbook.domain.catalog.flight

import com.dbook.domain.catalog.Airline
import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.Flight
import com.dbook.domain.catalog.SeatClass
import java.math.BigDecimal
import java.time.LocalDateTime

val airline = Airline(id = 1, iataCode = "LA", name = "LATAM")
val gru = Airport(1, "GRU", "Guarulhos", "São Paulo", "Brasil", "https://example.com/a.jpg", "América do Sul", false)
val gig = Airport(2, "GIG", "Galeão", "Rio de Janeiro", "Brasil", "https://example.com/b.jpg", "América do Sul", false)
val departure: LocalDateTime = LocalDateTime.of(2027, 2, 1, 8, 0)

fun aFlight(
    arrival: LocalDateTime = departure.plusHours(1),
    destination: Airport = gig,
    active: Boolean = true,
) = Flight(
    id = 5, title = "DB1 GRU-GIG", price = BigDecimal("100.00"), totalCapacity = 12, availableCapacity = 12,
    active = active, flightNumber = "DB1", airline = airline, origin = gru, destination = destination,
    departureTime = departure, arrivalTime = arrival, seatClass = SeatClass.ECONOMY, aircraftType = "Airbus A320",
)
