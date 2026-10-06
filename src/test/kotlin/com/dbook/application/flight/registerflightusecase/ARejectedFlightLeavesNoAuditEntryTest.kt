package com.dbook.application.flight.registerflightusecase

import com.dbook.domain.flight.AirlineNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ARejectedFlightLeavesNoAuditEntryTest : RegisterFlightUseCaseFixture() {
    @Test
    fun `given an unknown airline when registering then it fails and nothing is recorded`() {
        assertFailsWith<AirlineNotFoundException> { useCase.execute(command(airline = "XX")) }

        assertTrue(auditLog.events.isEmpty())
    }
}
