package com.dbook.presentation.audit

import com.dbook.application.audit.SearchAuditLogQuery
import com.dbook.application.audit.SearchAuditLogUseCase
import com.dbook.domain.audit.AuditFilter
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.common.audit.AuditOutcome
import java.time.Instant

/** The query string of `GET /admin/audit`: every filter is optional. */
data class AuditSearchRequest(
    val actorId: Long? = null,
    val action: AuditAction? = null,
    val targetType: String? = null,
    val targetId: String? = null,
    val outcome: AuditOutcome? = null,
    val from: Instant? = null,
    val to: Instant? = null,
    val cursor: String? = null,
    val size: Int = SearchAuditLogUseCase.DEFAULT_SIZE,
) {
    fun toQuery() =
        SearchAuditLogQuery(
            filter = AuditFilter(actorId, action, targetType, targetId, outcome, from, to),
            after = cursor?.let(AuditCursorCodec::decode),
            size = size,
        )
}
