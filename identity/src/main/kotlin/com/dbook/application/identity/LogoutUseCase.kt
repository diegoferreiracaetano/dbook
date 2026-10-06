package com.dbook.application.identity

import com.dbook.domain.identity.RefreshTokenRepository
import com.dbook.domain.identity.TokenService
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

@Observed(name = "dbook.usecase")
@Service
class LogoutUseCase(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val tokenService: TokenService,
) {
    // idempotent: an unknown or already revoked token is not an error
    fun execute(refreshToken: String) {
        val stored = refreshTokenRepository.findByTokenHash(tokenService.hashToken(refreshToken))
        if (stored != null && !stored.revoked) {
            refreshTokenRepository.revoke(requireNotNull(stored.id) { "A found refresh token must be persisted" })
        }
    }
}
