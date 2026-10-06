package com.dbook.infrastructure.web

import com.dbook.domain.audit.AuditContext
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

// what ties an audit entry to the logs and the trace of the request that caused it
@Component
class RequestAuditContext {
    fun current(): AuditContext {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        return AuditContext(
            requestId = MDC.get(RequestLoggingFilter.REQUEST_ID_KEY),
            traceId = MDC.get(TRACE_ID_KEY),
            ip = request?.remoteAddr,
            userAgent = request?.getHeader("User-Agent")?.take(MAX_USER_AGENT_LENGTH),
        )
    }

    private companion object {
        const val TRACE_ID_KEY = "traceId"
        const val MAX_USER_AGENT_LENGTH = 255
    }
}
