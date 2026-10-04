package com.dbook.application.booking.cancelbookingusecase

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class AStaffCancellationIsAuditedWithTheBeforeAndTheAfterTest : CancelBookingUseCaseFixture() {
    @Test
    fun `given another user's booking when staff cancels it then the audit has the actor and both states`() {
        withTransactionSynchronization {
            useCase.execute(bookingId, requestingUserId = 999, requestingUserRole = Role.SUPPORT)
        }

        val event = auditLog.events.single()
        assertEquals(AuditAction.BOOKING_CANCELLED_BY_STAFF, event.action)
        assertEquals(Actor(999, Role.SUPPORT), event.actor)
        assertEquals(bookingId.toString(), event.targetId)
        assertEquals("PENDING", event.before?.get("status"))
        assertEquals("CANCELLED", event.after?.get("status"))
    }
}
