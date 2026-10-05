package com.dbook.domain.booking.booking

import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.toAuditSnapshot
import com.dbook.domain.catalog.Airline
import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.Flight
import com.dbook.domain.catalog.SeatClass
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TheBookingSnapshotListsOnlyIdsAndTheStatusTest {
    @Test
    fun `given a booking when its audit snapshot is taken then it carries identifiers and the status only`() {
        val origin = Airport(1, "GRU", "Name", "City", "Country", "https://example.com/photo.jpg", "Region", false)
        val destination = Airport(2, "GIG", "Name", "City", "Country", "https://example.com/photo.jpg", "Region", false)
        val flight =
            Flight(
                id = 5,
                title = "DB1 GRU-GIG",
                price = BigDecimal("100.00"),
                totalCapacity = 1,
                availableCapacity = 1,
                flightNumber = "DB1",
                airline = Airline(id = 1, iataCode = "LA", name = "LATAM"),
                origin = origin,
                destination = destination,
                departureTime = LocalDateTime.of(2027, 2, 1, 8, 0),
                arrivalTime = LocalDateTime.of(2027, 2, 1, 9, 10),
                seatClass = SeatClass.ECONOMY,
                aircraftType = "Airbus A320",
            )

        val snapshot = Booking(id = 3, bookable = flight, seatId = 9, customerId = 4).toAuditSnapshot()

        assertEquals(
            mapOf("id" to 3L, "bookableId" to 5L, "seatId" to 9L, "customerId" to 4L, "status" to "PENDING"),
            snapshot,
        )
    }
}
