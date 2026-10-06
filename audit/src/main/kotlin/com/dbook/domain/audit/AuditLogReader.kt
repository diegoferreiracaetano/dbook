package com.dbook.domain.audit

interface AuditLogReader {
    fun search(
        filter: AuditFilter,
        after: AuditCursor?,
        limit: Int,
    ): AuditPage
}
