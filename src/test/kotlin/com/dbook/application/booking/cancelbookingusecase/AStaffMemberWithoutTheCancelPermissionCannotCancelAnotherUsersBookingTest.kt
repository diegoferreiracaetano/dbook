package com.dbook.application.booking.cancelbookingusecase

import com.dbook.domain.booking.NotBookingOwnerException
import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AStaffMemberWithoutTheCancelPermissionCannotCancelAnotherUsersBookingTest : CancelBookingUseCaseFixture() {
    @Test
    fun `given another user's booking when a CATALOG_MANAGER cancels it then it throws NotBookingOwnerException`() {
        withTransactionSynchronization {
            assertFailsWith<NotBookingOwnerException> {
                useCase.execute(bookingId, requestingUserId = 999, requestingUserRole = Role.CATALOG_MANAGER)
            }
        }
    }
}
