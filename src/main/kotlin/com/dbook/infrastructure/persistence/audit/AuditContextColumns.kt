package com.dbook.infrastructure.persistence.audit

import jakarta.persistence.Embeddable

// the request that caused the entry: the columns that tie it to the logs and the trace
@Embeddable
class AuditContextColumns(
    var requestId: String? = null,
    var traceId: String? = null,
    var ip: String? = null,
    var userAgent: String? = null,
)
