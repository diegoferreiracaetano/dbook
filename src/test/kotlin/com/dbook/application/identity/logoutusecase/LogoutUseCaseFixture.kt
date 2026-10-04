package com.dbook.application.identity.logoutusecase

import com.dbook.application.identity.LogoutUseCase
import com.dbook.application.identity.loginusecase.FakeRefreshTokenRepository
import com.dbook.application.identity.loginusecase.FakeTokenService
import com.dbook.domain.identity.RefreshToken
import java.time.Instant

abstract class LogoutUseCaseFixture {
    protected val refreshTokens = FakeRefreshTokenRepository()
    protected val useCase = LogoutUseCase(refreshTokens, FakeTokenService())

    protected fun givenASessionWithToken(token: String) {
        val expiresAt = Instant.now().plusSeconds(60)
        refreshTokens.save(RefreshToken(userId = 1, tokenHash = "hash:$token", expiresAt = expiresAt))
    }

    protected fun isSpent(token: String) = refreshTokens.findByTokenHash("hash:$token")?.revoked
}
