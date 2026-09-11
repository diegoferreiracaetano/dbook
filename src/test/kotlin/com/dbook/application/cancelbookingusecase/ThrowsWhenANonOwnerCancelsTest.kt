package com.dbook.application.cancelbookingusecase

import com.dbook.domain.NotBookingOwnerException
import com.dbook.domain.Role
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenANonOwnerCancelsTest : CancelBookingUseCaseFixture() {
    @Test
    fun `given another user's booking when a non-owner CLIENT cancels it then it throws NotBookingOwnerException`() {
        assertFailsWith<NotBookingOwnerException> {
            useCase.execute(bookingId, requestingUserId = 999, requestingUserRole = Role.CLIENT)
        }
    }
}
