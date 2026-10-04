package com.dbook.presentation.identity

import com.dbook.application.identity.LoginCommand
import com.dbook.application.identity.LoginUseCase
import com.dbook.application.identity.RefreshTokenUseCase
import com.dbook.application.identity.RegisterUserCommand
import com.dbook.application.identity.RegisterUserUseCase
import com.dbook.presentation.common.ApiPaths
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `POST /auth/register`, `/login`, `/refresh` — all public routes (see `SecurityConfig`). */
@RestController
@RequestMapping("${ApiPaths.V1}/auth")
@Tag(name = "Auth", description = "Registration, login and token refresh")
class AuthController(
    private val registerUserUseCase: RegisterUserUseCase,
    private val loginUseCase: LoginUseCase,
    private val refreshTokenUseCase: RefreshTokenUseCase,
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
}
