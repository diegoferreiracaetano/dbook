package com.dbook.application

import com.dbook.domain.InvalidTokenException
import com.dbook.domain.RefreshToken
import com.dbook.domain.RefreshTokenRepository
import com.dbook.domain.TokenService
import com.dbook.domain.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RefreshTokenUseCase(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val tokenService: TokenService,
) {
    // Rotation: a valid refresh token is revoked the moment it's exchanged — it's
    // single-use. An already invalid one (unknown, expired or already revoked) is just
    // rejected, with no extra write.
    fun execute(refreshToken: String): TokenPair {
        val stored = refreshTokenRepository.findByTokenHash(tokenService.hashToken(refreshToken))
        if (stored == null || !stored.isValid(Instant.now())) {
            throw InvalidTokenException()
        }
        refreshTokenRepository.revoke(requireNotNull(stored.id) { "A found refresh token must be persisted" })

        val user = userRepository.findById(stored.userId) ?: throw InvalidTokenException()
        val newAccessToken = tokenService.generateAccessToken(user)
        val newRefreshToken = tokenService.generateRefreshToken(user)
        refreshTokenRepository.save(
            RefreshToken(
                userId = stored.userId,
                tokenHash = tokenService.hashToken(newRefreshToken),
                expiresAt = tokenService.refreshTokenExpiresAt(),
            ),
        )
        return TokenPair(newAccessToken, newRefreshToken)
    }
}
