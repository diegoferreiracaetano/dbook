package com.dbook.infrastructure.web

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * The per-user twin of the per-IP limit, after the sign-in is known: one customer cannot hammer the API even from
 * many addresses, and many customers behind one address do not eat each other's allowance (the per-IP one is wide
 * for that). A rate of its own for the AI suggestions is kept apart (see [AiRateLimitInterceptor]).
 */
@Component
class UserRateLimitInterceptor(
    private val limits: RequestLimitsProperties,
    private val counter: FixedWindowCounter,
) : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = authentication?.takeIf { it.isAuthenticated && it !is AnonymousAuthenticationToken }?.name
        val over =
            limits.rateLimitEnabled && user != null && (counter.hit("user:$user") ?: 0L) > limits.perUserPerMinute
        if (over) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = "application/json"
            response.setHeader("Retry-After", counter.secondsLeftInWindow().toString())
            response.writer.write("""{"error":"Too many requests, try again later","code":"RATE_LIMITED"}""")
        }
        return !over
    }
}
