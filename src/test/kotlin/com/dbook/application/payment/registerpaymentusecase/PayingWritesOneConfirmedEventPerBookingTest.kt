package com.dbook.application.payment.registerpaymentusecase

import com.dbook.application.payment.RegisterPaymentCommand
import com.dbook.domain.booking.BookingEvents
import kotlin.test.Test
import kotlin.test.assertEquals

class PayingWritesOneConfirmedEventPerBookingTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given a round trip when it is paid then each leg has its own booking confirmed event`() {
        executeCommitted(
            RegisterPaymentCommand(
                bookingIds = listOf(outboundBookingId, returnBookingId),
                cardLast4 = "4242",
                cardholderName = "Jane Doe",
                requestingUserId = ownerId,
                idempotencyKey = "key-1",
            ),
        )

        assertEquals(listOf(BookingEvents.CONFIRMED, BookingEvents.CONFIRMED), outbox.events.map { it.type })
        assertEquals(
            setOf(outboundBookingId, returnBookingId),
            outbox.events.map { it.payload["bookingId"] }.toSet(),
        )
        assertEquals(setOf(ownerId), outbox.events.map { it.payload["customerId"] }.toSet())
    }

    @Test
    fun `given the same request repeated when it is replayed then no second event is written`() {
        val command =
            RegisterPaymentCommand(listOf(outboundBookingId), "4242", "Jane Doe", ownerId, "key-1")
        executeCommitted(command)

        executeCommitted(command)

        assertEquals(1, outbox.events.size)
    }
}
