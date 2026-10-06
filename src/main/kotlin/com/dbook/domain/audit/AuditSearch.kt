package com.dbook.domain.audit

import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditOutcome
import java.time.Instant

data class AuditFilter(
    val actorId: Long? = null,
    val action: AuditAction? = null,
    val targetType: String? = null,
    val targetId: String? = null,
    val outcome: AuditOutcome? = null,
    val from: Instant? = null,
    val to: Instant? = null,
)

data class AuditCursor(
    val occurredAt: Instant,
    val id: Long,
)

data class AuditPage(
    val entries: List<AuditEntry>,
    val next: AuditCursor?,
)
