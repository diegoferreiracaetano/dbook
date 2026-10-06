package com.dbook.domain.common.audit

interface AuditLog {
    fun record(event: AuditEvent)
}
