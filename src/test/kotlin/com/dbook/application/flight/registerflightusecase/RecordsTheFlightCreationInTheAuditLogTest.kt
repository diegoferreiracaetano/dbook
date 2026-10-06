package com.dbook.application.flight.registerflightusecase

import com.dbook.domain.audit.AuditAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecordsTheFlightCreationInTheAuditLogTest : RegisterFlightUseCaseFixture() {
    @Test
    fun `given a valid flight when registered then who created it and the resulting flight are recorded`() {
        val flight = useCase.execute(command())

        val event = auditLog.events.single()
        assertEquals(AuditAction.FLIGHT_CREATED, event.action)
        assertEquals(admin, event.actor)
        assertEquals(flight.id.toString(), event.targetId)
        assertNull(event.before)
        assertEquals("DB1234", event.after?.get("flightNumber"))
    }
}
