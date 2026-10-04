package com.dbook.application.audit.searchauditlogusecase

import com.dbook.application.audit.SearchAuditLogUseCase
import com.dbook.domain.audit.AuditCursor
import com.dbook.domain.audit.AuditFilter
import com.dbook.domain.audit.AuditLogReader
import com.dbook.domain.audit.AuditPage

class RecordingAuditLogReader : AuditLogReader {
    var lastFilter: AuditFilter? = null
    var lastAfter: AuditCursor? = null
    var lastLimit: Int? = null

    override fun search(
        filter: AuditFilter,
        after: AuditCursor?,
        limit: Int,
    ): AuditPage {
        lastFilter = filter
        lastAfter = after
        lastLimit = limit
        return AuditPage(emptyList(), null)
    }
}

abstract class SearchAuditLogUseCaseFixture {
    protected val reader = RecordingAuditLogReader()
    protected val useCase = SearchAuditLogUseCase(reader)
}
