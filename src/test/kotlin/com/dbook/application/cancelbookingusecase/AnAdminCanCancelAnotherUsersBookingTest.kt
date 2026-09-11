package com.dbook.application.cancelbookingusecase

import com.dbook.domain.BookingStatus
import com.dbook.domain.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class AnAdminCanCancelAnotherUsersBookingTest : CancelBookingUseCaseFixture() {
    @Test
    fun `given another user's booking when an ADMIN cancels it then it moves to CANCELLED`() {
        withTransactionSynchronization {
            val cancelled = useCase.execute(bookingId, requestingUserId = 999, requestingUserRole = Role.ADMIN)

            assertEquals(BookingStatus.CANCELLED, cancelled.status)
        }
    }
}
