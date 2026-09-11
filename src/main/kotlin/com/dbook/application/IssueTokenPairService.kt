package com.dbook.application

import com.dbook.domain.RefreshToken
import com.dbook.domain.RefreshTokenRepository
import com.dbook.domain.TokenService
import com.dbook.domain.User
import org.springframework.stereotype.Service

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)

/**
 * Shared by [LoginUseCase] and [RefreshTokenUseCase] — both need to generate a fresh
 * access+refresh pair for a user and persist the refresh token the same way.
 */
@Service
class IssueTokenPairService(
    private val tokenService: TokenService,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    fun issueFor(user: User): TokenPair {
        val userId = requireNotNull(user.id) { "Cannot issue tokens for a user that hasn't been persisted" }
        val accessToken = tokenService.generateAccessToken(user)
        val refreshToken = tokenService.generateRefreshToken(user)
        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                tokenHash = tokenService.hashToken(refreshToken),
                expiresAt = tokenService.refreshTokenExpiresAt(),
            ),
        )
        return TokenPair(accessToken, refreshToken)
    }
}
