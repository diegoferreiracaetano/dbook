package com.dbook.domain.catalog.flight

import com.dbook.domain.catalog.Airline
import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.Flight
import com.dbook.domain.catalog.SeatClass
import com.dbook.domain.catalog.toAuditSnapshot
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

// An allow-list: a field added to Flight tomorrow reaches the audit trail only if someone adds it here on purpose.
class TheFlightSnapshotListsOnlyTheAllowedFieldsTest {
    private fun airport(iata: String) =
        Airport(
            id = 1,
            iataCode = iata,
            name = "Name",
            city = "City",
            country = "Country",
            photoUrl = "https://example.com/photo.jpg",
            region = "Region",
            isPopular = false,
        )

    @Test
    fun `given a flight when its audit snapshot is taken then only the allowed fields appear, dates as text`() {
        val flight =
            Flight(
                id = 7,
                title = "DB1 GRU-GIG",
                price = BigDecimal("500.00"),
                totalCapacity = 180,
                availableCapacity = 180,
                flightNumber = "DB1",
                airline = Airline(id = 1, iataCode = "LA", name = "LATAM"),
                origin = airport("GRU"),
                destination = airport("GIG"),
                departureTime = LocalDateTime.of(2027, 2, 1, 8, 0),
                arrivalTime = LocalDateTime.of(2027, 2, 1, 9, 10),
                seatClass = SeatClass.ECONOMY,
                aircraftType = "Airbus A320",
            )

        val snapshot = flight.toAuditSnapshot()

        assertEquals(
            setOf(
                "id", "flightNumber", "airline", "origin", "destination", "departureTime", "arrivalTime",
                "seatClass", "price", "totalCapacity", "aircraftType", "status",
            ),
            snapshot.keys,
        )
        assertEquals("2027-02-01T08:00", snapshot["departureTime"])
        assertEquals("LA", snapshot["airline"])
    }
}
