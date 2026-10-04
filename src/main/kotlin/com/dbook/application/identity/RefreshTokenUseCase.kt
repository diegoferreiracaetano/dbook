package com.dbook.application.identity

import com.dbook.domain.identity.AccountBlockedException
import com.dbook.domain.identity.InvalidTokenException
import com.dbook.domain.identity.RefreshTokenRepository
import com.dbook.domain.identity.TokenService
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import java.time.Clock

/** Exchanges a valid refresh token for a new access/refresh pair — single-use rotation, see [execute]. */
@Observed(name = "dbook.usecase")
@Service
class RefreshTokenUseCase(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val tokenService: TokenService,
    private val issueTokenPairService: IssueTokenPairService,
    private val clock: Clock,
) {
    // Rotation: a valid refresh token is revoked the moment it's exchanged — it's single-use. Every check
    // that can still reject happens before the token is spent, so a refused request leaves the session as it was.
    fun execute(
        refreshToken: String,
        audience: SessionAudience = SessionAudience.CLIENT,
    ): TokenPair {
        val stored = refreshTokenRepository.findByTokenHash(tokenService.hashToken(refreshToken))
        if (stored == null || !stored.isValid(clock.instant())) {
            throw InvalidTokenException()
        }
        val user = requireUsable(userRepository.findById(stored.userId), audience)
        refreshTokenRepository.revoke(requireNotNull(stored.id) { "A found refresh token must be persisted" })
        return issueTokenPairService.issueFor(user)
    }

    // a token from the other front door looks exactly like an invalid one
    private fun requireUsable(
        user: User?,
        audience: SessionAudience,
    ): User {
        if (user == null || !audience.accepts(user.role)) {
            throw InvalidTokenException()
        }
        if (user.isBlocked) {
            throw AccountBlockedException()
        }
        return user
    }
}
