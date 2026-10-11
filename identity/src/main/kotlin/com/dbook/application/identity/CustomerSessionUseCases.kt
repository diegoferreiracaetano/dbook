package com.dbook.application.identity

import com.dbook.domain.identity.RefreshToken
import com.dbook.domain.identity.RefreshTokenRepository
import com.dbook.domain.identity.SessionNotFoundException
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

/** The caller's signed-in sessions: one per device that still holds a valid refresh token. */
@Observed(name = "dbook.usecase")
@Service
class ListMySessionsUseCase(
    private val refreshTokens: RefreshTokenRepository,
    private val clock: Clock,
) {
    fun execute(userId: Long): List<RefreshToken> = refreshTokens.findActiveByUser(userId, clock.instant())
}

/** Ends one of the caller's sessions; someone else's (or an unknown one) is "not found", never "forbidden". */
@Observed(name = "dbook.usecase")
@Service
class EndMySessionUseCase(
    private val refreshTokens: RefreshTokenRepository,
    private val clock: Clock,
) {
    @Transactional
    fun execute(
        userId: Long,
        familyId: String,
    ) {
        val owned = refreshTokens.findActiveByUser(userId, clock.instant()).any { it.familyId == familyId }
        if (!owned) throw SessionNotFoundException(familyId)
        refreshTokens.revokeFamily(familyId)
    }
}
