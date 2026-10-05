package com.dbook.application.booking.cancelbookingusecase

import com.dbook.domain.booking.BookingEvents
import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AStaffCancellationWritesAnEventTheCustomerIsToldAboutTest : CancelBookingUseCaseFixture() {
    @Test
    fun `given another user's booking when staff cancels it then a booking cancelled-by-staff event is written`() {
        withTransactionSynchronization {
            useCase.execute(bookingId, requestingUserId = 999, requestingUserRole = Role.SUPPORT)
        }

        val event = outbox.events.single()
        assertEquals(BookingEvents.CANCELLED_BY_STAFF, event.type)
        assertEquals(ownerId, event.payload["customerId"])
        assertEquals(bookingId, event.payload["bookingId"])
        assertEquals(now, event.availableAt)
    }

    @Test
    fun `given the owner cancelling their own booking when it is cancelled then no event is written`() {
        withTransactionSynchronization {
            useCase.execute(bookingId, requestingUserId = ownerId, requestingUserRole = Role.CLIENT)
        }

        assertTrue(outbox.events.isEmpty())
    }
}
