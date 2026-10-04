package com.dbook.application.booking.cancelbookingusecase

import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class AStaffMemberWithTheCancelPermissionCanCancelAnotherUsersBookingTest : CancelBookingUseCaseFixture() {
    @Test
    fun `given another user's booking when a SUPPORT agent cancels it then it moves to CANCELLED`() {
        withTransactionSynchronization {
            val cancelled = useCase.execute(bookingId, requestingUserId = 999, requestingUserRole = Role.SUPPORT)

            assertEquals(BookingStatus.CANCELLED, cancelled.status)
        }
    }
}
