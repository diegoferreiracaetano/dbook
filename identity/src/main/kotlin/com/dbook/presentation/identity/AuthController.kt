package com.dbook.presentation.identity

import com.dbook.application.identity.LoginCommand
import com.dbook.application.identity.LoginUseCase
import com.dbook.application.identity.RefreshTokenUseCase
import com.dbook.application.identity.RegisterUserCommand
import com.dbook.application.identity.RegisterUserUseCase
import com.dbook.application.identity.RequestPasswordResetUseCase
import com.dbook.application.identity.ResendEmailVerificationUseCase
import com.dbook.application.identity.ResetPasswordCommand
import com.dbook.application.identity.ResetPasswordUseCase
import com.dbook.application.identity.VerifyEmailUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * `POST /auth/register`, `/login`, `/refresh` and the account recovery routes (`/verify-email`, `/forgot-password`,
 * `/reset-password`) are public (see `SecurityConfig`); `/resend-verification` needs the signed-in customer.
 */
@RestController
@RequestMapping("${ApiPaths.V1}/auth")
@Tag(name = "Auth", description = "Registration, login and token refresh")
class AuthController(
    private val registerUserUseCase: RegisterUserUseCase,
    private val loginUseCase: LoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val verifyEmailUseCase: VerifyEmailUseCase,
    private val resendVerificationUseCase: ResendEmailVerificationUseCase,
    private val requestPasswordResetUseCase: RequestPasswordResetUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
) {
    @Operation(summary = "Registers a new user as CLIENT")
    @PostMapping("/register")
    fun register(
        @RequestBody request: RegisterUserRequest,
    ): ResponseEntity<UserResponse> {
        val user =
            registerUserUseCase.execute(
                RegisterUserCommand(request.email, request.password, request.name),
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user))
    }

    @Operation(summary = "Logs in, returning an access token and a refresh token")
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        servletRequest: HttpServletRequest,
    ): TokenResponse =
        TokenResponse.from(
            loginUseCase.execute(LoginCommand(request.email, request.password, servletRequest.remoteAddr)),
        )

    @Operation(summary = "Exchanges a refresh token for a new token pair, rotating the old one")
    @PostMapping("/refresh")
    fun refresh(
        @RequestBody request: RefreshRequest,
    ): TokenResponse = TokenResponse.from(refreshTokenUseCase.execute(request.refreshToken))

    @Operation(summary = "Confirms the e-mail address with the link that was mailed (single use, 48 hours)")
    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun verifyEmail(
        @RequestBody request: VerifyEmailRequest,
    ) = verifyEmailUseCase.execute(request.token)

    @Operation(summary = "Mails a new confirmation link to the signed-in customer (the old one stops working)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/resend-verification")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun resendVerification(
        authentication: Authentication,
        servletRequest: HttpServletRequest,
    ) = resendVerificationUseCase.execute(authentication.currentUserId(), servletRequest.remoteAddr)

    @Operation(
        summary = "Mails a link to choose a new password",
        description = "Always 202, whether or not the address has an account: the answer reveals nothing about it",
    )
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun forgotPassword(
        @RequestBody request: ForgotPasswordRequest,
        servletRequest: HttpServletRequest,
    ) = requestPasswordResetUseCase.execute(request.email, servletRequest.remoteAddr)

    @Operation(summary = "Chooses the new password with the mailed link (single use, 1 hour) and ends every session")
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun resetPassword(
        @RequestBody request: ResetPasswordRequest,
    ) = resetPasswordUseCase.execute(ResetPasswordCommand(request.token, request.newPassword))
}
