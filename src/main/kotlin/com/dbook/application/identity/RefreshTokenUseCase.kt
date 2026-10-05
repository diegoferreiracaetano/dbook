package com.dbook.application.identity

import com.dbook.application.common.countOutcome
import com.dbook.domain.identity.AccountBlockedException
import com.dbook.domain.identity.InvalidTokenException
import com.dbook.domain.identity.RefreshToken
import com.dbook.domain.identity.RefreshTokenRepository
import com.dbook.domain.identity.TokenService
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import io.micrometer.core.instrument.MeterRegistry
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
    private val meterRegistry: MeterRegistry,
) {
    // Rotation: a valid refresh token is revoked the moment it's exchanged — it's single-use. Every check
    // that can still reject happens before the token is spent, so a refused request leaves the session as it was.
    fun execute(
        refreshToken: String,
        audience: SessionAudience = SessionAudience.CLIENT,
    ): TokenPair {
        val stored = refreshTokenRepository.findByTokenHash(tokenService.hashToken(refreshToken))
        if (stored == null || !stored.isValid(clock.instant())) {
            stored?.takeIf { it.revoked }?.let { reuseDetected(it) }
            throw InvalidTokenException()
        }
        val user = requireUsable(userRepository.findById(stored.userId), audience)
        // atomic: of two refreshes of the same token at once, one wins; the other is a reuse
        if (!refreshTokenRepository.consume(requireNotNull(stored.id) { "A found refresh token must be persisted" })) {
            reuseDetected(stored)
            throw InvalidTokenException()
        }
        return issueTokenPairService.issueFor(user, stored.familyId)
    }

    // A spent token presented again is a copy somebody kept: every token of its sign-in is revoked, so whoever holds
    // one (the thief, and the real user too) has to sign in again.
    private fun reuseDetected(token: RefreshToken) {
        refreshTokenRepository.revokeFamily(token.familyId)
        meterRegistry.countOutcome("dbook.auth.refresh", "reuse_detected")
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
