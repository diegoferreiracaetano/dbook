package com.dbook.infrastructure.security

import com.dbook.domain.identity.Role
import com.dbook.domain.identity.TokenService
import com.dbook.domain.identity.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val tokenService: TokenService,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val token = header.removePrefix("Bearer ")
            val userId = tokenService.parseUserId(token)
            val role = tokenService.parseRole(token)
            if (userId != null && role != null) {
                authenticate(request, userId, role)
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun authenticate(
        request: HttpServletRequest,
        userId: Long,
        tokenRole: Role,
    ) {
        val role = if (isAdminRoute(request)) currentStaffRole(userId) else tokenRole
        if (role != null) {
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(userId.toString(), null, role.toAuthorities())
            // every log line of this request now says who it was (cleared by RequestLoggingFilter)
            MDC.put("userId", userId.toString())
        }
    }

    // On admin routes the role and status come from the database, not from the token: a staff member who is
    // blocked or demoted loses access on the very next request instead of when the token expires.
    private fun currentStaffRole(userId: Long): Role? =
        userRepository.findById(userId)?.takeUnless { it.isBlocked }?.role

    private fun isAdminRoute(request: HttpServletRequest) = request.requestURI.startsWith(ADMIN_PREFIX)

    private companion object {
        const val ADMIN_PREFIX = "/v1/admin/"
    }
}
