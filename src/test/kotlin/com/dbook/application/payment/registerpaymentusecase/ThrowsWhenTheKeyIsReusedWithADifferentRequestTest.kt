package com.dbook.application.payment.registerpaymentusecase

import com.dbook.domain.payment.IdempotencyKeyReusedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ThrowsWhenTheKeyIsReusedWithADifferentRequestTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given a round trip paid with a key when retried in another booking order then it is the same request`() {
        useCase.execute(command(bookingIds = listOf(outboundBookingId)))

        assertFailsWith<IdempotencyKeyReusedException> {
            useCase.execute(command(bookingIds = listOf(returnBookingId)))
        }
        assertEquals(1, paymentRepository.saved.size)
    }
}
