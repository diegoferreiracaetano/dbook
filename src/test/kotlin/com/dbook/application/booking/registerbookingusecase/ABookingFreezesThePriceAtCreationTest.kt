package com.dbook.application.booking.registerbookingusecase

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class ABookingFreezesThePriceAtCreationTest : RegisterBookingUseCaseFixture() {
    @Test
    fun `given a flight priced at 500 when a booking is registered then the booking carries that price`() {
        withTransactionSynchronization {
            val booking = useCase.execute(command())

            assertEquals(BigDecimal("500.00"), booking.price)
        }
    }
}
