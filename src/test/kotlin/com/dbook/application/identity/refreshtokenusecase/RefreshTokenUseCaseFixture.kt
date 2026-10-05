package com.dbook.application.identity.refreshtokenusecase

import com.dbook.application.identity.IssueTokenPairService
import com.dbook.application.identity.RefreshTokenUseCase
import com.dbook.application.identity.SessionAudience
import com.dbook.application.identity.loginusecase.FakeRefreshTokenRepository
import com.dbook.application.identity.loginusecase.FakeTokenService
import com.dbook.application.identity.loginusecase.SingleUserRepository
import com.dbook.domain.identity.RefreshToken
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

abstract class RefreshTokenUseCaseFixture {
    protected open val existingUser =
        User(id = 1, email = "diego@example.com", passwordHash = "x", name = "Diego", role = Role.CLIENT)

    protected val now: Instant = Instant.parse("2026-10-04T12:00:00Z")
    protected val refreshTokens = FakeRefreshTokenRepository()
    protected val meters = SimpleMeterRegistry()
    protected val useCase by lazy {
        refreshTokens.save(RefreshToken(userId = 1, tokenHash = "hash:valid-token", expiresAt = now.plusSeconds(3600)))
        RefreshTokenUseCase(
            refreshTokens,
            SingleUserRepository(existingUser),
            FakeTokenService(),
            IssueTokenPairService(FakeTokenService(), refreshTokens),
            Clock.fixed(now, ZoneOffset.UTC),
            meters,
        )
    }

    protected fun refresh(
        token: String = "valid-token",
        audience: SessionAudience = SessionAudience.CLIENT,
    ) = useCase.execute(token, audience)

    protected fun tokenIsSpent() = refreshTokens.saved.first { it.tokenHash == "hash:valid-token" }.revoked
}
