package com.dbook.application.expirebookingusecase

import com.dbook.domain.BookingStatus
import com.dbook.domain.SeatStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class DoesNothingWhenTheBookingWasAlreadyPaidTest : ExpireBookingUseCaseFixture() {
    @Test
    fun `given a booking paid before the deadline when the expiration message arrives then nothing changes`() {
        bookingRepository.save(requireNotNull(bookingRepository.findById(bookingId)).confirm(paymentId = 1L))

        withTransactionSynchronization { expireBookingUseCase.execute(bookingId) }

        assertEquals(BookingStatus.CONFIRMED, bookingRepository.findById(bookingId)?.status)
        assertEquals(SeatStatus.RESERVED, seatRepository.findById(seatId)?.status)
    }
}
