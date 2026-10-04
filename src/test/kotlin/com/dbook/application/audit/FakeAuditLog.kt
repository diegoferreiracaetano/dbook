package com.dbook.application.audit

import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.audit.AuditLog

class FakeAuditLog : AuditLog {
    val events = mutableListOf<AuditEvent>()

    override fun record(event: AuditEvent) {
        events += event
    }
}
