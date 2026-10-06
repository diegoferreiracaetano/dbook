package com.dbook.presentation.identity

import com.dbook.application.identity.ConfirmTwoFactorEnrollmentAtLoginUseCase
import com.dbook.application.identity.LoginCommand
import com.dbook.application.identity.LogoutUseCase
import com.dbook.application.identity.RefreshTokenUseCase
import com.dbook.application.identity.SessionAudience
import com.dbook.application.identity.StaffLoginOutcome
import com.dbook.application.identity.StaffLoginUseCase
import com.dbook.application.identity.StartTwoFactorEnrollmentAtLoginUseCase
import com.dbook.application.identity.TokenPair
import com.dbook.application.identity.VerifyTwoFactorCommand
import com.dbook.application.identity.VerifyTwoFactorLoginUseCase
import com.dbook.domain.identity.InvalidTokenException
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.PublicEndpoints
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
    private val staffLoginUseCase: StaffLoginUseCase,
    private val verifyTwoFactorUseCase: VerifyTwoFactorLoginUseCase,
    private val startEnrollmentUseCase: StartTwoFactorEnrollmentAtLoginUseCase,
    private val confirmEnrollmentUseCase: ConfirmTwoFactorEnrollmentAtLoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val sessionCookie: AdminSessionCookie,
    private val originPolicy: AdminPortalOriginPolicy,
) {
    @Operation(
        summary = "Logs a staff member in; a client's credentials are refused like a wrong password",
        description = "200 with the session, or 202 with a challenge when the account needs its second factor",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            content = [Content(schema = Schema(implementation = AdminSessionResponse::class))],
        ),
        ApiResponse(
            responseCode = "202",
            content = [Content(schema = Schema(implementation = TwoFactorChallengeResponse::class))],
        ),
    )
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        servletRequest: HttpServletRequest,
    ): ResponseEntity<*> =
        when (
            val outcome =
                staffLoginUseCase.execute(
                    LoginCommand(request.email, request.password, servletRequest.remoteAddr, SessionAudience.STAFF),
                )
        ) {
            is StaffLoginOutcome.Session -> session(outcome.tokens)
            is StaffLoginOutcome.Challenge ->
                ResponseEntity.accepted()
                    .body(TwoFactorChallengeResponse(outcome.challengeToken, outcome.enrollmentRequired))
        }

    @Operation(
        summary = "The second step of the sign-in: the challenge and a code (or a recovery code) open the session",
    )
    @PostMapping("/2fa/verify")
    fun verifyTwoFactor(
        @RequestBody request: TwoFactorVerifyRequest,
        servletRequest: HttpServletRequest,
    ): ResponseEntity<AdminSessionResponse> =
        session(
            verifyTwoFactorUseCase.execute(
                VerifyTwoFactorCommand(request.challengeToken, request.code, servletRequest.remoteAddr),
            ),
        )

    @Operation(summary = "Where the role requires a second factor and there is none: starts the enrollment")
    @PostMapping("/2fa/enroll")
    fun enrollTwoFactor(
        @RequestBody request: TwoFactorChallengeRequest,
    ): TwoFactorEnrollmentResponse =
        TwoFactorEnrollmentResponse.from(startEnrollmentUseCase.execute(request.challengeToken))

    @Operation(summary = "Finishes that enrollment with the first code: opens the session and shows the recovery codes")
    @PostMapping("/2fa/confirm")
    fun confirmTwoFactor(
        @RequestBody request: TwoFactorVerifyRequest,
        servletRequest: HttpServletRequest,
    ): ResponseEntity<AdminEnrolledSessionResponse> {
        val enrolled =
            confirmEnrollmentUseCase.execute(
                VerifyTwoFactorCommand(request.challengeToken, request.code, servletRequest.remoteAddr),
            )
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, sessionCookie.issue(enrolled.tokens.refreshToken).toString())
            .body(AdminEnrolledSessionResponse(enrolled.tokens.accessToken, enrolled.recoveryCodes))
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
