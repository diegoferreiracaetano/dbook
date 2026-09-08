package com.dbook.application

import com.dbook.domain.InvalidCredentialsException
import com.dbook.domain.PasswordHasher
import com.dbook.domain.RefreshToken
import com.dbook.domain.RefreshTokenRepository
import com.dbook.domain.TokenService
import com.dbook.domain.UserRepository
import org.springframework.stereotype.Service

data class LoginCommand(
    val email: String,
    val password: String,
)

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)

@Service
class LoginUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val tokenService: TokenService,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    fun execute(command: LoginCommand): TokenPair {
        val user = userRepository.findByEmail(command.email) ?: throw InvalidCredentialsException()
        if (!passwordHasher.matches(command.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        val accessToken = tokenService.generateAccessToken(user)
        val refreshToken = tokenService.generateRefreshToken(user)
        refreshTokenRepository.save(
            RefreshToken(
                userId = requireNotNull(user.id) { "A found user must be persisted" },
                tokenHash = tokenService.hashToken(refreshToken),
                expiresAt = tokenService.refreshTokenExpiresAt(),
            ),
        )
        return TokenPair(accessToken, refreshToken)
    }
}
