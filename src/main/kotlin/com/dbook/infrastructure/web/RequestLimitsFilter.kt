package com.dbook.infrastructure.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * The first two doors of the API, before anything is parsed or authenticated:
 * - **body size:** a declared `Content-Length` above the limit is a 413 right away (a JSON body of the API is a few
 *   kilobytes; only the flight CSV import is allowed to be big);
 * - **rate per IP:** more than the configured requests in a minute from one address is a 429 with `Retry-After`.
 *   Where the app runs behind a proxy, the address is the proxy's until forwarded headers are trusted explicitly.
 * Both answer in the API's own error format.
 */
@Component
@Order(RequestLimitsFilter.ORDER)
class RequestLimitsFilter(
    private val limits: RequestLimitsProperties,
    private val counter: FixedWindowCounter,
) : OncePerRequestFilter() {
    // the WebSocket handshake and the docs are not API traffic
    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI.let { it.startsWith("/ws") || it.startsWith("/v1/ws") || it.startsWith("/swagger-ui") }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val maxBody = if (request.requestURI.endsWith(IMPORT_PATH)) limits.maxImportBodyBytes else limits.maxBodyBytes
        when {
            request.contentLengthLong > maxBody ->
                reject(response, HttpStatus.PAYLOAD_TOO_LARGE, "The request body is too large", "PAYLOAD_TOO_LARGE")
            limits.rateLimitEnabled && tooManyFrom(request) ->
                reject(response, HttpStatus.TOO_MANY_REQUESTS, "Too many requests, try again later", "RATE_LIMITED")
            else -> filterChain.doFilter(request, response)
        }
    }

    private fun tooManyFrom(request: HttpServletRequest): Boolean =
        (counter.hit("ip:${request.remoteAddr}") ?: 0L) > limits.perIpPerMinute

    private fun reject(
        response: HttpServletResponse,
        status: HttpStatus,
        message: String,
        code: String,
    ) {
        response.status = status.value()
        response.contentType = "application/json"
        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            response.setHeader("Retry-After", counter.secondsLeftInWindow().toString())
        }
        response.writer.write("""{"error":"$message","code":"$code"}""")
    }

    companion object {
        // right after the app client filter (HIGHEST_PRECEDENCE + 3), before security
        const val ORDER = Ordered.HIGHEST_PRECEDENCE + 4
        private const val IMPORT_PATH = "/admin/flights/import"
    }
}
