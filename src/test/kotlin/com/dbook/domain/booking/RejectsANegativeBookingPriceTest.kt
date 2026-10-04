package com.dbook.domain.booking

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsANegativeBookingPriceTest : BookingTestFixture() {
    @Test
    fun `given a negative price when building the booking then it throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            Booking(bookable = flight, seatId = seatId, customerId = 1, price = BigDecimal("-1.00"))
        }
    }
}
