package com.dbook.infrastructure.persistence.audit

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditFilter
import com.dbook.domain.audit.AuditOutcome
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchFiltersByActionOutcomeAndPeriodTest : AuditLogRepositoryAdapterFixture() {
    @Test
    fun `given entries of two kinds when filtering then only the matching ones come back`() {
        val actorId = uniqueActorId()
        auditLog.record(event(actorId, AuditAction.FLIGHT_CREATED, AuditOutcome.SUCCESS))
        auditLog.record(
            event(actorId, AuditAction.ACCESS_DENIED, AuditOutcome.DENIED, targetId = "GET /v1/admin/audit"),
        )
        val now = Instant.now()

        fun count(filter: AuditFilter) = auditLogReader.search(filter, null, 100).entries.size

        assertEquals(2, count(AuditFilter(actorId = actorId)))
        assertEquals(1, count(AuditFilter(actorId = actorId, action = AuditAction.ACCESS_DENIED)))
        assertEquals(1, count(AuditFilter(actorId = actorId, outcome = AuditOutcome.SUCCESS)))
        assertEquals(1, count(AuditFilter(actorId = actorId, targetType = "ENDPOINT")))
        assertEquals(0, count(AuditFilter(actorId = actorId, from = now.plus(Duration.ofHours(1)))))
        assertEquals(0, count(AuditFilter(actorId = actorId, to = now.minus(Duration.ofHours(1)))))
    }
}
