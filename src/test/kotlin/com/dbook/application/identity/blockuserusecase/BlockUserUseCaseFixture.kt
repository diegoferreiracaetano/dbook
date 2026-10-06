package com.dbook.application.identity.blockuserusecase

import com.dbook.application.identity.BlockUserUseCase
import com.dbook.application.identity.UnblockUserUseCase
import com.dbook.application.identity.loginusecase.FakeRefreshTokenRepository
import com.dbook.application.identity.loginusecase.SingleUserRepository
import com.dbook.domain.common.access.Role.CLIENT
import com.dbook.domain.identity.User
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

abstract class BlockUserUseCaseFixture {
    protected val now: Instant = Instant.parse("2026-10-04T12:00:00Z")
    protected val refreshTokens = FakeRefreshTokenRepository()
    protected val users =
        SingleUserRepository(User(id = 1, email = "diego@example.com", passwordHash = "x", name = "D", role = CLIENT))
    protected val block = BlockUserUseCase(users, refreshTokens, Clock.fixed(now, ZoneOffset.UTC))
    protected val unblock = UnblockUserUseCase(users)
}
