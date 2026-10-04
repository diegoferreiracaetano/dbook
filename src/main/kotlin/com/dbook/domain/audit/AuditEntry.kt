package com.dbook.domain.audit

import java.time.Instant

data class AuditContext(
    val requestId: String?,
    val traceId: String?,
    val ip: String?,
    val userAgent: String?,
)

data class AuditEntry(
    val id: Long,
    val occurredAt: Instant,
    val event: AuditEvent,
    val context: AuditContext,
)
