package com.dbook.domain.audit

import com.dbook.domain.identity.Actor

data class AuditEvent(
    val actor: Actor,
    val action: AuditAction,
    val targetId: String,
    val outcome: AuditOutcome = AuditOutcome.SUCCESS,
    val before: Map<String, Any?>? = null,
    val after: Map<String, Any?>? = null,
    val reason: String? = null,
) {
    init {
        require(targetId.isNotBlank()) { "targetId must not be blank" }
    }

    val targetType: String get() = action.target
}
