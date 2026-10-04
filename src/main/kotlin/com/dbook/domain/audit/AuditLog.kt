package com.dbook.domain.audit

interface AuditLog {
    fun record(event: AuditEvent)
}
