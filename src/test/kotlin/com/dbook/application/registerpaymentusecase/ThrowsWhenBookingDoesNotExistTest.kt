package com.dbook.application.registerpaymentusecase

import com.dbook.application.RegisterPaymentCommand
import com.dbook.domain.BookingNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenBookingDoesNotExistTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given a bookingId that doesn't exist when paying then it throws BookingNotFoundException`() {
        assertFailsWith<BookingNotFoundException> {
            useCase.execute(
                RegisterPaymentCommand(
                    bookingIds = listOf(999),
                    cardLast4 = "4242",
                    cardholderName = "Jane Doe",
                    requestingUserId = ownerId,
                ),
            )
        }
    }
}
