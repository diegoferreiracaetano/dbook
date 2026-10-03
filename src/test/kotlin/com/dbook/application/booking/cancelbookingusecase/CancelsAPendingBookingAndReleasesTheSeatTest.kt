package com.dbook.application.booking.cancelbookingusecase

import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.identity.Role
import com.dbook.domain.seating.SeatStatus
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
