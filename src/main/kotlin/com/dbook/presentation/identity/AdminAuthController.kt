package com.dbook.presentation.identity

import com.dbook.application.identity.LoginCommand
import com.dbook.application.identity.LoginUseCase
import com.dbook.application.identity.LogoutUseCase
import com.dbook.application.identity.RefreshTokenUseCase
import com.dbook.application.identity.SessionAudience
import com.dbook.application.identity.TokenPair
import com.dbook.domain.identity.InvalidTokenException
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.PublicEndpoints
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `POST /admin/auth/login`, `/refresh`, `/logout` — open routes, because they are what creates the session. */
@RestController
@RequestMapping("${ApiPaths.V1}/admin/auth")
@Tag(name = "Admin auth", description = "Admin portal session: access token in the body, refresh token in a cookie")
@PublicEndpoints
class AdminAuthController(
    private val loginUseCase: LoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val sessionCookie: AdminSessionCookie,
    private val originPolicy: AdminPortalOriginPolicy,
) {
    @Operation(summary = "Logs a staff member in; a client's credentials are refused like a wrong password")
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        servletRequest: HttpServletRequest,
    ): ResponseEntity<AdminSessionResponse> {
        val tokens =
            loginUseCase.execute(
                LoginCommand(request.email, request.password, servletRequest.remoteAddr, SessionAudience.STAFF),
            )
        return session(tokens)
    }

    @Operation(summary = "Renews the session from the refresh cookie, rotating it (only from the portal's origin)")
    @PostMapping("/refresh")
    fun refresh(
        @RequestHeader(HttpHeaders.ORIGIN, required = false) origin: String?,
        @CookieValue(AdminSessionCookie.NAME, required = false) refreshToken: String?,
    ): ResponseEntity<AdminSessionResponse> {
        originPolicy.require(origin)
        return session(
            refreshTokenUseCase.execute(refreshToken ?: throw InvalidTokenException(), SessionAudience.STAFF),
        )
    }

    @Operation(summary = "Ends the session: revokes the refresh token and clears the cookie")
    @PostMapping("/logout")
    fun logout(
        @RequestHeader(HttpHeaders.ORIGIN, required = false) origin: String?,
        @CookieValue(AdminSessionCookie.NAME, required = false) refreshToken: String?,
    ): ResponseEntity<Unit> {
        originPolicy.require(origin)
        refreshToken?.let(logoutUseCase::execute)
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, sessionCookie.clear().toString()).build()
    }

    private fun session(tokens: TokenPair): ResponseEntity<AdminSessionResponse> =
        ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, sessionCookie.issue(tokens.refreshToken).toString())
            .body(AdminSessionResponse(tokens.accessToken))
}
