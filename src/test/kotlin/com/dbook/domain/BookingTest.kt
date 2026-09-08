package com.dbook.domain

import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BookingTest {
    private val flight =
        Flight(
            id = 1,
            title = "GRU-GIG",
            price = BigDecimal("500.00"),
            totalCapacity = 180,
            availableCapacity = 179,
            flightNumber = "DB1234",
            origin =
                Airport(id = 1, iataCode = "GRU", name = "Guarulhos", city = "São Paulo", country = "Brasil"),
            destination =
                Airport(id = 2, iataCode = "GIG", name = "Galeão", city = "Rio de Janeiro", country = "Brasil"),
            departureTime = LocalDateTime.of(2026, 10, 1, 8, 0),
            arrivalTime = LocalDateTime.of(2026, 10, 1, 9, 10),
            seatClass = SeatClass.ECONOMY,
        )

    @Test
    fun `a new booking starts as PENDING`() {
        val booking = Booking(bookable = flight, customerId = 1)
        assertEquals(BookingStatus.PENDING, booking.status)
    }

    @Test
    fun `a PENDING booking can be confirmed`() {
        val booking = Booking(bookable = flight, customerId = 1).confirm()
        assertEquals(BookingStatus.CONFIRMED, booking.status)
    }

    @Test
    fun `a PENDING booking can be cancelled`() {
        val booking = Booking(bookable = flight, customerId = 1).cancel()
        assertEquals(BookingStatus.CANCELLED, booking.status)
    }

    @Test
    fun `a CONFIRMED booking cannot be confirmed again`() {
        val confirmed = Booking(bookable = flight, customerId = 1).confirm()
        assertFailsWith<IllegalStateException> { confirmed.confirm() }
    }

    @Test
    fun `a CANCELLED booking cannot be cancelled again`() {
        val cancelled = Booking(bookable = flight, customerId = 1).cancel()
        assertFailsWith<IllegalStateException> { cancelled.cancel() }
    }
}
