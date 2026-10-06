package com.dbook.application.audit

import com.dbook.domain.common.audit.AuditEvent
import com.dbook.domain.common.audit.AuditLog

class FakeAuditLog : AuditLog {
    val events = mutableListOf<AuditEvent>()

    override fun record(event: AuditEvent) {
        events += event
    }
}
