package com.dbook.domain.booking

import com.dbook.domain.accommodation.Accommodation
import com.dbook.domain.catalog.Airport
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ABookingIsOfASeatOrOfAStayNeverBothTest {
    private val gig = Airport(2, "GIG", "Galeão", "Rio", "Brasil", "https://example.com/p.jpg", "América do Sul", true)
    private val hotel =
        Accommodation(id = 1, name = "Copacabana", destination = gig, address = "Av. Atlântica", stars = 4)
    private val stay = Stay(1, LocalDate.of(2027, 1, 15), LocalDate.of(2027, 1, 17), 2, BigDecimal("300.00"))

    @Test
    fun `given a stay and no seat when a booking is made then it is valid and keeps its stay through every step`() {
        val booking = Booking(bookable = hotel, seatId = null, customerId = 1, price = stay.total, stay = stay)

        assertEquals(stay, booking.confirm(paymentId = 9).stay)
        assertEquals(stay, booking.cancel().stay)
        assertEquals(stay, booking.confirm(9).refund().stay)
    }

    @Test
    fun `given both a seat and a stay or neither when a booking is made then it is refused`() {
        assertFailsWith<IllegalArgumentException> {
            Booking(bookable = hotel, seatId = 5, customerId = 1, price = BigDecimal.TEN, stay = stay)
        }
        assertFailsWith<IllegalArgumentException> {
            Booking(bookable = hotel, seatId = null, customerId = 1, price = BigDecimal.TEN)
        }
    }
}
