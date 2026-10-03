package com.dbook.application.payment.registerpaymentusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class TreatsTheSameBookingsInAnyOrderAsTheSameRequestTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given a key used for one booking when reused for another then it throws IdempotencyKeyReusedException`() {
        val first = executeCommitted(command(bookingIds = listOf(outboundBookingId, returnBookingId)))
        val retry = executeCommitted(command(bookingIds = listOf(returnBookingId, outboundBookingId)))

        assertEquals(first.id, retry.id)
        assertEquals(1, paymentRepository.saved.size)
    }
}
