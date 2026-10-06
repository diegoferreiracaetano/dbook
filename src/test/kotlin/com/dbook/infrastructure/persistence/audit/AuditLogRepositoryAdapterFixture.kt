package com.dbook.infrastructure.persistence.audit

import com.dbook.AbstractIntegrationTest
import com.dbook.domain.audit.AuditFilter
import com.dbook.domain.audit.AuditLogReader
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.access.Role
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditEvent
import com.dbook.domain.common.audit.AuditLog
import com.dbook.domain.common.audit.AuditOutcome
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

abstract class AuditLogRepositoryAdapterFixture : AbstractIntegrationTest() {
    @Autowired
    lateinit var auditLog: AuditLog

    @Autowired
    lateinit var auditLogReader: AuditLogReader

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    // every test works with its own actor id, so the shared database never mixes their entries
    protected fun uniqueActorId() = (1_000_000L..999_999_999L).random()

    protected fun event(
        actorId: Long,
        action: AuditAction = AuditAction.FLIGHT_CREATED,
        outcome: AuditOutcome = AuditOutcome.SUCCESS,
        targetId: String = "1",
    ) = AuditEvent(Actor(actorId, Role.SUPER_ADMIN), action, targetId, outcome)

    protected fun entriesOf(actorId: Long) = auditLogReader.search(AuditFilter(actorId = actorId), null, 100).entries
}
