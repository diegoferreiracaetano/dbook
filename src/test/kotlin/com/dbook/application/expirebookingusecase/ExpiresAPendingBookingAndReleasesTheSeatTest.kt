package com.dbook.application.expirebookingusecase

import com.dbook.domain.BookingStatus
import com.dbook.domain.SeatStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpiresAPendingBookingAndReleasesTheSeatTest : ExpireBookingUseCaseFixture() {
    @Test
    fun `given a pending booking when it expires then it is cancelled and its seat is released`() {
        withTransactionSynchronization { expireBookingUseCase.execute(bookingId) }

        assertEquals(BookingStatus.CANCELLED, bookingRepository.findById(bookingId)?.status)
        assertEquals(SeatStatus.AVAILABLE, seatRepository.findById(seatId)?.status)
    }
}
