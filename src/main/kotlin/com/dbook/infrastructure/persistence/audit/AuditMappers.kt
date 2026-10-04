package com.dbook.infrastructure.persistence.audit

import com.dbook.domain.audit.AuditContext
import com.dbook.domain.audit.AuditEntry
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.identity.Actor
import java.time.Instant

fun AuditEvent.toJpaEntity(
    occurredAt: Instant,
    context: AuditContext,
): AuditLogJpaEntity =
    AuditLogJpaEntity(
        occurredAt = occurredAt,
        actorId = actor.id,
        actorRole = actor.role,
        action = action,
        outcome = outcome,
        targetType = targetType,
        targetId = targetId,
        stateBefore = before?.toMutableMap(),
        stateAfter = after?.toMutableMap(),
        reason = reason,
        context = AuditContextColumns(context.requestId, context.traceId, context.ip, context.userAgent),
    )

fun AuditLogJpaEntity.toDomain(): AuditEntry =
    AuditEntry(
        id = requireNotNull(id),
        occurredAt = occurredAt,
        event =
            AuditEvent(
                actor = Actor(actorId, actorRole),
                action = action,
                targetId = targetId,
                outcome = outcome,
                before = stateBefore,
                after = stateAfter,
                reason = reason,
            ),
        context = AuditContext(context.requestId, context.traceId, context.ip, context.userAgent),
    )
