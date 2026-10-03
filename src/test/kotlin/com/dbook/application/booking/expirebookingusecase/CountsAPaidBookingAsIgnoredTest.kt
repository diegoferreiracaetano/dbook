package com.dbook.application.booking.expirebookingusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class CountsAPaidBookingAsIgnoredTest : ExpireBookingUseCaseFixture() {
    @Test
    fun `given a booking paid in time when its expiration arrives then it is counted as ignored`() {
        bookingRepository.save(requireNotNull(bookingRepository.findById(bookingId)).confirm(paymentId = 1L))

        withTransactionSynchronization { expireBookingUseCase.execute(bookingId) }

        assertEquals(1.0, counted("dbook.booking.expiration", "ignored"))
        assertEquals(0.0, counted("dbook.booking.expiration", "expired"))
    }
}
