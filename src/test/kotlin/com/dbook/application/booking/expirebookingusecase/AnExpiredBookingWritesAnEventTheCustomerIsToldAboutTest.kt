package com.dbook.application.booking.expirebookingusecase

import com.dbook.domain.booking.BookingEvents
import kotlin.test.Test
import kotlin.test.assertEquals

class AnExpiredBookingWritesAnEventTheCustomerIsToldAboutTest : ExpireBookingUseCaseFixture() {
    @Test
    fun `given a pending booking when it expires then a booking expired event is written for its owner`() {
        withTransactionSynchronization { expireBookingUseCase.execute(bookingId) }

        val event = outbox.events.single()
        assertEquals(BookingEvents.EXPIRED, event.type)
        assertEquals(ownerId, event.payload["customerId"])
        assertEquals("GRU-GIG", event.payload["title"])
    }
}
