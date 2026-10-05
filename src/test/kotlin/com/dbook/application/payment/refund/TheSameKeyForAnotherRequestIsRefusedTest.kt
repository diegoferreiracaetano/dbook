package com.dbook.application.payment.refund

import com.dbook.domain.payment.IdempotencyKeyReusedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TheSameKeyForAnotherRequestIsRefusedTest : RefundUseCaseFixture() {
    @Test
    fun `given a key already used for another booking when refunding then it is refused, not replayed`() {
        committed { refundBooking.execute(command(bookingId = 100)) }

        assertFailsWith<IdempotencyKeyReusedException> { committed { refundBooking.execute(command(bookingId = 101)) } }

        assertEquals(1, gateway.calls.size)
    }
}
