package com.dbook.application.payment.registerpaymentusecase

import com.dbook.application.payment.RegisterPaymentCommand
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenABookingIsNotPendingTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given an already-cancelled booking when paying for it then it throws IllegalStateException`() {
        val cancelled = bookingRepository.findById(outboundBookingId)!!.cancel()
        bookingRepository.save(cancelled)

        assertFailsWith<IllegalStateException> {
            executeCommitted(
                RegisterPaymentCommand(
                    bookingIds = listOf(outboundBookingId),
                    cardLast4 = "4242",
                    cardholderName = "Jane Doe",
                    requestingUserId = ownerId,
                    idempotencyKey = "key-1",
                ),
            )
        }
    }
}
