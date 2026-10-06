package com.dbook.application.booking.cancelbookingusecase

import com.dbook.domain.booking.BookingNotFoundException
import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenBookingDoesNotExistTest : CancelBookingUseCaseFixture() {
    @Test
    fun `given a nonexistent bookingId when cancelling then it throws BookingNotFoundException`() {
        assertFailsWith<BookingNotFoundException> {
            useCase.execute(999, ownerId, Role.CLIENT)
        }
    }
}
