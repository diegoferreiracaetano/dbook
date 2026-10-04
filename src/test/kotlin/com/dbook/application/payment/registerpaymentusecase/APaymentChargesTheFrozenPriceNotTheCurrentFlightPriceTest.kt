package com.dbook.application.payment.registerpaymentusecase

import com.dbook.domain.booking.Booking
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

// The reason Booking.price exists: the flight now costs 500, but this booking was made when it cost 400.
class APaymentChargesTheFrozenPriceNotTheCurrentFlightPriceTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given a booking made at 400 on a flight now priced at 500 when paid then the payment is 400`() {
        val bookingId = 300L
        bookingRepository.save(
            Booking(bookingId, outboundFlight, seatId = 30, customerId = ownerId, price = BigDecimal("400.00")),
        )

        val payment = executeCommitted(command(bookingIds = listOf(bookingId)))

        assertEquals(BigDecimal("400.00"), payment.amount)
    }
}
