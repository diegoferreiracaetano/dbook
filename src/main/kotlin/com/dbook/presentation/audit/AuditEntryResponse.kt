package com.dbook.presentation.audit

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEntry
import com.dbook.domain.audit.AuditOutcome
import com.dbook.domain.identity.Role
import java.time.Instant

data class AuditEntryResponse(
    val id: Long,
    val occurredAt: Instant,
    val actorId: Long,
    val actorRole: Role,
    val action: AuditAction,
    val outcome: AuditOutcome,
    val targetType: String,
    val targetId: String,
    val before: Map<String, Any?>?,
    val after: Map<String, Any?>?,
    val reason: String?,
    val requestId: String?,
    val traceId: String?,
    val ip: String?,
    val userAgent: String?,
) {
    companion object {
        fun from(entry: AuditEntry) =
            AuditEntryResponse(
                id = entry.id,
                occurredAt = entry.occurredAt,
                actorId = entry.event.actor.id,
                actorRole = entry.event.actor.role,
                action = entry.event.action,
                outcome = entry.event.outcome,
                targetType = entry.event.targetType,
                targetId = entry.event.targetId,
                before = entry.event.before,
                after = entry.event.after,
                reason = entry.event.reason,
                requestId = entry.context.requestId,
                traceId = entry.context.traceId,
                ip = entry.context.ip,
                userAgent = entry.context.userAgent,
            )
    }
}
