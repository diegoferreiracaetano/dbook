package com.dbook.domain.booking

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

// A booking priced differently from its flight (the flight was repriced after it) keeps its own price.
class TheFrozenPriceSurvivesConfirmingAndCancellingTest : BookingTestFixture() {
    @Test
    fun `given a booking priced below its flight when confirmed or cancelled then its price does not change`() {
        val booking = Booking(bookable = flight, seatId = seatId, customerId = 1, price = BigDecimal("400.00"))

        assertEquals(BigDecimal("400.00"), booking.confirm(paymentId = 1).price)
        assertEquals(BigDecimal("400.00"), booking.cancel().price)
    }
}
