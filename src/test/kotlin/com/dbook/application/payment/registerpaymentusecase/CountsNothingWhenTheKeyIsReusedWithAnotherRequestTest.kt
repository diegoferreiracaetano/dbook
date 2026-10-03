package com.dbook.application.payment.registerpaymentusecase

import com.dbook.domain.payment.IdempotencyKeyReusedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CountsNothingWhenTheKeyIsReusedWithAnotherRequestTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given a key reused for another request when it is rejected then nothing new is counted`() {
        executeCommitted(command(bookingIds = listOf(outboundBookingId)))

        assertFailsWith<IdempotencyKeyReusedException> {
            executeCommitted(command(bookingIds = listOf(returnBookingId)))
        }

        assertEquals(1.0, counted("dbook.payment", "created"))
        assertEquals(0.0, counted("dbook.payment", "replayed"))
    }
}
