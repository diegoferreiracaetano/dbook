package com.dbook.application.booking.cancelbookingusecase

import com.dbook.domain.booking.NotBookingOwnerException
import com.dbook.domain.identity.Role
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
