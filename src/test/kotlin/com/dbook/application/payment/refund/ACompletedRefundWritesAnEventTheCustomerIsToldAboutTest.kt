package com.dbook.application.payment.refund

import com.dbook.domain.payment.RefundEvents
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class ACompletedRefundWritesAnEventTheCustomerIsToldAboutTest : RefundUseCaseFixture() {
    @Test
    fun `given a confirmed booking when the refund completes then a refund completed event carries the amount`() {
        committed { refundBooking.execute(command()) }

        val event = outbox.events.single()
        assertEquals(RefundEvents.COMPLETED, event.type)
        assertEquals(3L, event.payload["customerId"])
        assertEquals(BigDecimal("500.00").toPlainString(), event.payload["amount"])
    }

    @Test
    fun `given a gateway failure when the refund is attempted then no event is written`() {
        gateway.failure = "gateway refuses"

        committed { refundBooking.execute(command()) }

        assertEquals(0, outbox.events.size)
    }
}
