package com.dbook.application.registerpaymentusecase

import com.dbook.application.RegisterPaymentCommand
import com.dbook.domain.NotBookingOwnerException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenANonOwnerPaysTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given another user's booking when a non-owner pays for it then it throws NotBookingOwnerException`() {
        assertFailsWith<NotBookingOwnerException> {
            useCase.execute(
                RegisterPaymentCommand(
                    bookingIds = listOf(outboundBookingId),
                    cardLast4 = "4242",
                    cardholderName = "Jane Doe",
                    requestingUserId = 999,
                ),
            )
        }
    }
}
