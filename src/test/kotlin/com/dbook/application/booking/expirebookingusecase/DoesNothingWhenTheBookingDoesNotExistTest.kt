package com.dbook.application.booking.expirebookingusecase

import com.dbook.domain.booking.BookingStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class DoesNothingWhenTheBookingDoesNotExistTest : ExpireBookingUseCaseFixture() {
    @Test
    fun `given an unknown booking id when the expiration message arrives then it returns without failing`() {
        withTransactionSynchronization { expireBookingUseCase.execute(999L) }

        assertEquals(BookingStatus.PENDING, bookingRepository.findById(bookingId)?.status)
    }
}
