package com.dbook.infrastructure.cache

import com.dbook.domain.catalog.Airline
import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.Flight
import com.dbook.domain.catalog.SeatClass
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class FlightSnapshotKeepsEveryFieldOfTheFlightTest {
    private fun airport(
        id: Long,
        code: String,
    ) = Airport(id, code, "Name $code", "City $code", "Brasil", "https://example.com/$code.jpg", "Sul", id == 1L)

    @Test
    fun `given flights when written and read back then every field is the same`() {
        val flights =
            listOf(
                Flight(
                    id = 7,
                    title = "DB1 GRU-GIG",
                    price = BigDecimal("500.50"),
                    totalCapacity = 180,
                    availableCapacity = 42,
                    active = false,
                    flightNumber = "DB1",
                    airline = Airline(3, "LA", "LATAM"),
                    origin = airport(1, "GRU"),
                    destination = airport(2, "GIG"),
                    departureTime = LocalDateTime.of(2027, 3, 1, 8, 5),
                    arrivalTime = LocalDateTime.of(2027, 3, 1, 9, 15),
                    seatClass = SeatClass.BUSINESS,
                    aircraftType = "Airbus A320",
                ),
            )
        val snapshot = FlightSnapshot(ObjectMapper())

        val back = snapshot.read(snapshot.write(flights)).single()
        val original = flights.single()

        assertEquals(original.id, back.id)
        assertEquals(original.title, back.title)
        assertEquals(original.price, back.price)
        assertEquals(original.totalCapacity, back.totalCapacity)
        assertEquals(original.availableCapacity, back.availableCapacity)
        assertEquals(original.active, back.active)
        assertEquals(original.flightNumber, back.flightNumber)
        assertEquals(original.departureTime, back.departureTime)
        assertEquals(original.arrivalTime, back.arrivalTime)
        assertEquals(original.seatClass, back.seatClass)
        assertEquals(original.aircraftType, back.aircraftType)
        assertEquals(original.airline.iataCode to original.airline.name, back.airline.iataCode to back.airline.name)
        listOf(original.origin to back.origin, original.destination to back.destination).forEach { (a, b) ->
            assertEquals(
                listOf(a.id, a.iataCode, a.name, a.city, a.country, a.photoUrl, a.region, a.isPopular),
                listOf(b.id, b.iataCode, b.name, b.city, b.country, b.photoUrl, b.region, b.isPopular),
            )
        }
    }

    @Test
    fun `given no flights when written and read back then it is an empty list and not a miss`() {
        val snapshot = FlightSnapshot(ObjectMapper())

        assertEquals(emptyList(), snapshot.read(snapshot.write(emptyList())))
    }
}
