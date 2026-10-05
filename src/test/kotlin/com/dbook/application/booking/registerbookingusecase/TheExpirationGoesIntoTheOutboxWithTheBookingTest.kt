package com.dbook.application.booking.registerbookingusecase

import com.dbook.domain.booking.BookingEvents
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class TheExpirationGoesIntoTheOutboxWithTheBookingTest : RegisterBookingUseCaseFixture() {
    @Test
    fun `given a booking when it is made then its expiration is an outbox event due in 15 minutes`() {
        val bookingId = withTransactionSynchronizationResult { requireNotNull(useCase.execute(command()).id) }

        val event = outbox.events.single()
        assertEquals(BookingEvents.EXPIRATION_REQUESTED, event.type)
        assertEquals("booking", event.aggregateType)
        assertEquals(bookingId.toString(), event.aggregateId)
        assertEquals(mapOf("bookingId" to bookingId), event.payload)
        assertEquals(now.plus(Duration.ofMinutes(15)), event.availableAt)
    }
}
