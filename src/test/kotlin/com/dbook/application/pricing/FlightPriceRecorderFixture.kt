package com.dbook.application.pricing

import com.dbook.application.common.RecordingOutboxWriter
import com.dbook.domain.catalog.Airport
import com.dbook.domain.flight.Airline
import com.dbook.domain.flight.Flight
import com.dbook.domain.flight.SeatClass
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

// The clock is 2026-10-04 12:00. Flight 5 goes GRU-GIG on 2027-01-15.
abstract class FlightPriceRecorderFixture {
    protected val now: Instant = Instant.parse("2026-10-04T12:00:00Z")
    protected val history = InMemoryPriceHistory()
    protected val outbox = RecordingOutboxWriter()
    protected val recorder = FlightPriceRecorder(history, outbox, Clock.fixed(now, ZoneOffset.UTC))

    private fun airport(
        id: Long,
        code: String,
    ) = Airport(id, code, "Airport $code", "City", "Brasil", "https://example.com/p.jpg", "América do Sul", false)

    protected fun flightPricedAt(price: String) =
        Flight(
            id = 5, title = "DB1 GRU-GIG", price = BigDecimal(price), totalCapacity = 1, availableCapacity = 1,
            flightNumber = "DB1", airline = Airline(1, "LA", "LATAM"), origin = airport(1, "GRU"),
            destination = airport(2, "GIG"), departureTime = LocalDateTime.of(2027, 1, 15, 8, 0),
            arrivalTime = LocalDateTime.of(2027, 1, 15, 9, 0), seatClass = SeatClass.ECONOMY, aircraftType = "A320",
        )
}
