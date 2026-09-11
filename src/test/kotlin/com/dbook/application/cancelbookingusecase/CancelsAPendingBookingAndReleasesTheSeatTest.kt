package com.dbook.application.cancelbookingusecase

import com.dbook.domain.BookingStatus
import com.dbook.domain.Role
import com.dbook.domain.SeatStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class CancelsAPendingBookingAndReleasesTheSeatTest : CancelBookingUseCaseFixture() {
    @Test
    fun `given a PENDING booking when the owner cancels it then it becomes CANCELLED and the seat AVAILABLE`() {
        withTransactionSynchronization {
            val cancelled = useCase.execute(bookingId, ownerId, Role.CLIENT)

            assertEquals(BookingStatus.CANCELLED, cancelled.status)
            assertEquals(SeatStatus.AVAILABLE, seatRepository.findById(seatId)!!.status)
        }
    }
}
