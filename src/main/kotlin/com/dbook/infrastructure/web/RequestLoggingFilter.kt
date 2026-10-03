package com.dbook.infrastructure.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Gives every request an id and writes one access line when it ends.
 *
 * The id (`requestId` in the MDC, echoed in the `X-Request-Id` response header) ties together
 * every log line of one request: filter by it to see everything that happened. A caller may
 * send its own, but only if it is short and plain — anything else is replaced, so a header can
 * never inject line breaks or arbitrary text into the logs.
 *
 * Deliberately logged: method, path, status and duration. Never: the query string (it can carry
 * tokens), headers (Authorization), or bodies (card data, passwords).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestLoggingFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    // no API docs noise in the logs
    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI.startsWith("/swagger-ui") || request.requestURI.startsWith("/v3/api-docs")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId =
            request.getHeader(REQUEST_ID_HEADER)?.takeIf(VALID_REQUEST_ID::matches)
                ?: UUID.randomUUID().toString()
        MDC.put(REQUEST_ID_KEY, requestId)
        response.setHeader(REQUEST_ID_HEADER, requestId)
        val startedAt = System.nanoTime()
        try {
            filterChain.doFilter(request, response)
        } finally {
            logAccess(request, response, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt))
            MDC.remove(REQUEST_ID_KEY)
            MDC.remove(USER_ID_KEY)
        }
    }

    // the kv(...) arguments become separate JSON fields in the json profile, so the access
    // lines can be filtered and aggregated by status or duration instead of parsed from text
    private fun logAccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        durationMs: Long,
    ) {
        val method = request.method
        val path = request.requestURI
        val status = response.status
        if (status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error(
                "{} {} -> {} in {} ms",
                method,
                path,
                status,
                durationMs,
                kv("status", status),
                kv("duration_ms", durationMs),
            )
        } else {
            log.info(
                "{} {} -> {} in {} ms",
                method,
                path,
                status,
                durationMs,
                kv("status", status),
                kv("duration_ms", durationMs),
            )
        }
    }

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-Id"
        const val REQUEST_ID_KEY = "requestId"
        const val USER_ID_KEY = "userId"
        private val VALID_REQUEST_ID = Regex("^[A-Za-z0-9._-]{1,64}$")
    }
}
