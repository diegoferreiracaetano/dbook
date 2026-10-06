package com.dbook.application.audit

import com.dbook.domain.audit.AuditCursor
import com.dbook.domain.audit.AuditFilter
import com.dbook.domain.audit.AuditLogReader
import com.dbook.domain.audit.AuditPage
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

data class SearchAuditLogQuery(
    val filter: AuditFilter = AuditFilter(),
    val after: AuditCursor? = null,
    val size: Int = SearchAuditLogUseCase.DEFAULT_SIZE,
)

@Observed(name = "dbook.usecase")
@Service
class SearchAuditLogUseCase(
    private val auditLogReader: AuditLogReader,
) {
    fun execute(query: SearchAuditLogQuery): AuditPage {
        require(query.size in 1..MAX_SIZE) { "size must be between 1 and $MAX_SIZE" }
        return auditLogReader.search(query.filter, query.after, query.size)
    }

    companion object {
        const val DEFAULT_SIZE = 50
        const val MAX_SIZE = 100
    }
}
