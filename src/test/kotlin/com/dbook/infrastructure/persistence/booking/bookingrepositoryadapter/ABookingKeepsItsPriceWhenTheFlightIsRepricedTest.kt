package com.dbook.infrastructure.persistence.booking.bookingrepositoryadapter

import com.dbook.application.booking.bookingconcurrency.BookingConcurrencyFixture
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

// Against the real PostgreSQL: the price is a column of the booking, so changing the flight's price afterwards
// must not reach it. Compared with compareTo because NUMERIC(12, 2) may come back with another scale.
class ABookingKeepsItsPriceWhenTheFlightIsRepricedTest : BookingConcurrencyFixture() {
    @Test
    fun `given a booking made at 100 when the flight is repriced to 150 then the booking still says 100`() {
        val userId = registerUser()
        val bookableId = registerFlight(totalCapacity = 1)
        val bookingId = bookSeat(bookableId, seatIdsOf(bookableId).single(), userId)

        jdbcTemplate.update("UPDATE bookable SET price = ? WHERE id = ?", BigDecimal("150.00"), bookableId)

        val booking = requireNotNull(bookingRepository.findById(bookingId))
        assertEquals(0, BigDecimal("100.00").compareTo(booking.price))
        assertEquals(0, BigDecimal("150.00").compareTo(booking.bookable.price))
    }
}
