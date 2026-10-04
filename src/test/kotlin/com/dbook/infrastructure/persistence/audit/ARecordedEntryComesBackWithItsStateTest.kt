package com.dbook.infrastructure.persistence.audit

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ARecordedEntryComesBackWithItsStateTest : AuditLogRepositoryAdapterFixture() {
    @Test
    fun `given an event with before and after when recorded then it is read back with both and its moment`() {
        val actorId = uniqueActorId()
        auditLog.record(
            AuditEvent(
                actor = Actor(actorId, Role.SUPPORT),
                action = AuditAction.BOOKING_CANCELLED_BY_STAFF,
                targetId = "12",
                before = mapOf("status" to "PENDING"),
                after = mapOf("status" to "CANCELLED"),
                reason = "customer asked",
            ),
        )

        val entry = entriesOf(actorId).single()

        assertNotNull(entry.occurredAt)
        assertEquals(Role.SUPPORT, entry.event.actor.role)
        assertEquals("BOOKING", entry.event.targetType)
        assertEquals("12", entry.event.targetId)
        assertEquals(mapOf("status" to "PENDING"), entry.event.before)
        assertEquals(mapOf("status" to "CANCELLED"), entry.event.after)
        assertEquals("customer asked", entry.event.reason)
        assertNull(entry.context.requestId)
    }
}
