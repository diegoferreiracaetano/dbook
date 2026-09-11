package com.dbook.presentation

import com.dbook.application.LoginCommand
import com.dbook.application.LoginUseCase
import com.dbook.application.RefreshTokenUseCase
import com.dbook.application.RegisterUserCommand
import com.dbook.application.RegisterUserUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** `POST /auth/register`, `/login`, `/refresh` — all public routes (see `SecurityConfig`). */
@RestController
@RequestMapping("/auth")
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
        val user = registerUserUseCase.execute(RegisterUserCommand(request.email, request.password))
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user))
    }

    @Operation(summary = "Logs in, returning an access token and a refresh token")
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
    ): TokenResponse = TokenResponse.from(loginUseCase.execute(LoginCommand(request.email, request.password)))

    @Operation(summary = "Exchanges a refresh token for a new token pair, rotating the old one")
    @PostMapping("/refresh")
    fun refresh(
        @RequestBody request: RefreshRequest,
    ): TokenResponse = TokenResponse.from(refreshTokenUseCase.execute(request.refreshToken))
}
