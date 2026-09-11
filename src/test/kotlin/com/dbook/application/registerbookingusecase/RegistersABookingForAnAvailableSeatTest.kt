package com.dbook.application.registerbookingusecase

import com.dbook.domain.BookingStatus
import com.dbook.domain.SeatStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class RegistersABookingForAnAvailableSeatTest : RegisterBookingUseCaseFixture() {
    @Test
    fun `given an available seat when a booking is registered then it starts PENDING and the seat becomes RESERVED`() {
        withTransactionSynchronization {
            val booking = useCase.execute(command())

            assertEquals(BookingStatus.PENDING, booking.status)
            assertEquals(SeatStatus.RESERVED, seatRepository.findById(seatId)!!.status)
        }
    }
}
