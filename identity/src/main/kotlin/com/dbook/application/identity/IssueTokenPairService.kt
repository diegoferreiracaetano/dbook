package com.dbook.application.identity

import com.dbook.domain.identity.RefreshToken
import com.dbook.domain.identity.RefreshTokenRepository
import com.dbook.domain.identity.TokenService
import com.dbook.domain.identity.User
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import java.util.UUID

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
)

/**
 * Shared by [LoginUseCase] and [RefreshTokenUseCase] — both need to generate a fresh
 * access+refresh pair for a user and persist the refresh token the same way.
 */
@Observed(name = "dbook.usecase")
@Service
class IssueTokenPairService(
    private val tokenService: TokenService,
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    // [familyId]: the chain this pair continues (a refresh); a new sign-in starts a family of its own
    fun issueFor(
        user: User,
        familyId: String = UUID.randomUUID().toString(),
    ): TokenPair {
        val userId = requireNotNull(user.id) { "Cannot issue tokens for a user that hasn't been persisted" }
        val accessToken = tokenService.generateAccessToken(user)
        val refreshToken = tokenService.generateRefreshToken(user)
        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                tokenHash = tokenService.hashToken(refreshToken),
                expiresAt = tokenService.refreshTokenExpiresAt(),
                familyId = familyId,
            ),
        )
        return TokenPair(accessToken, refreshToken)
    }
}
