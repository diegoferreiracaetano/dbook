package com.dbook.domain.booking

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class NewBookingFreezesTheBookablePriceTest : BookingTestFixture() {
    @Test
    fun `given a bookable priced at 500 when a booking is created then it carries that price`() {
        val booking = Booking(bookable = flight, seatId = seatId, customerId = 1)

        assertEquals(BigDecimal("500.00"), booking.price)
    }
}
