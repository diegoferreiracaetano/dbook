package com.dbook.application.booking.cancelbookingusecase

import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertTrue

// The trail is for administrative actions: a customer cancelling their own booking is not one.
class AnOwnersCancellationIsNotAuditedTest : CancelBookingUseCaseFixture() {
    @Test
    fun `given a booking when its owner cancels it then nothing is recorded in the audit log`() {
        withTransactionSynchronization { useCase.execute(bookingId, ownerId, Role.CLIENT) }

        assertTrue(auditLog.events.isEmpty())
    }
}
