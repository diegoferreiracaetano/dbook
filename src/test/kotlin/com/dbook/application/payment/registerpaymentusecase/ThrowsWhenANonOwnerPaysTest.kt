package com.dbook.application.payment.registerpaymentusecase

import com.dbook.application.payment.RegisterPaymentCommand
import com.dbook.domain.booking.NotBookingOwnerException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenANonOwnerPaysTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given another user's booking when a non-owner pays for it then it throws NotBookingOwnerException`() {
        assertFailsWith<NotBookingOwnerException> {
            executeCommitted(
                RegisterPaymentCommand(
                    bookingIds = listOf(outboundBookingId),
                    cardLast4 = "4242",
                    cardholderName = "Jane Doe",
                    requestingUserId = 999,
                    idempotencyKey = "key-1",
                ),
            )
        }
    }
}
