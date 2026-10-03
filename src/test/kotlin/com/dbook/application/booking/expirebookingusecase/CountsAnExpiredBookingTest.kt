package com.dbook.application.booking.expirebookingusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class CountsAnExpiredBookingTest : ExpireBookingUseCaseFixture() {
    @Test
    fun `given a pending booking when it expires then it is counted as expired and not as ignored`() {
        withTransactionSynchronization { expireBookingUseCase.execute(bookingId) }

        assertEquals(1.0, counted("dbook.booking.expiration", "expired"))
        assertEquals(0.0, counted("dbook.booking.expiration", "ignored"))
    }
}
