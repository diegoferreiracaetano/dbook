package com.dbook.infrastructure.security

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.audit.AuditLog
import com.dbook.domain.audit.AuditOutcome
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.Role
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

@Component
class JsonAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(
            objectMapper.writeValueAsString(mapOf("error" to "Authentication required", "code" to "UNAUTHORIZED")),
        )
    }
}

@Component
class JsonAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
    private val auditLog: AuditLog,
) : AccessDeniedHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        auditDenial(request)
        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(objectMapper.writeValueAsString(mapOf("error" to "Access denied", "code" to "FORBIDDEN")))
    }

    // Someone who is logged in reached for the admin area without the permission: a security signal worth a
    // record. Failing to write it must never change the answer, which is still a 403.
    private fun auditDenial(request: HttpServletRequest) {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !request.requestURI.startsWith(ADMIN_PREFIX)) {
            return
        }
        try {
            auditLog.record(
                AuditEvent(
                    actor = authentication.toActor(),
                    action = AuditAction.ACCESS_DENIED,
                    targetId = "${request.method} ${request.requestURI}".take(MAX_TARGET_LENGTH),
                    outcome = AuditOutcome.DENIED,
                ),
            )
        } catch (ex: DataAccessException) {
            log.warn("Could not record a denied admin access", ex)
        }
    }

    private fun Authentication.toActor() =
        Actor(
            id = name.toLong(),
            role = Role.valueOf(authorities.first { it.authority.startsWith("ROLE_") }.authority.removePrefix("ROLE_")),
        )

    private companion object {
        const val ADMIN_PREFIX = "/v1/admin/"
        const val MAX_TARGET_LENGTH = 100
    }
}
