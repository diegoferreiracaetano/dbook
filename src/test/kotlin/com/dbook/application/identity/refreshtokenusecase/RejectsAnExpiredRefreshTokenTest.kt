package com.dbook.application.identity.refreshtokenusecase

import com.dbook.application.identity.IssueTokenPairService
import com.dbook.application.identity.RefreshTokenUseCase
import com.dbook.application.identity.loginusecase.FakeRefreshTokenRepository
import com.dbook.application.identity.loginusecase.FakeTokenService
import com.dbook.application.identity.loginusecase.SingleUserRepository
import com.dbook.domain.identity.InvalidTokenException
import java.time.Clock
import java.time.Duration
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsAnExpiredRefreshTokenTest : RefreshTokenUseCaseFixture() {
    @Test
    fun `given a refresh token past its expiry when refreshing then it throws InvalidTokenException`() {
        useCase
        val twoHoursLater = Clock.offset(Clock.fixed(now, ZoneOffset.UTC), Duration.ofHours(2))
        val lateUseCase =
            RefreshTokenUseCase(
                refreshTokens,
                SingleUserRepository(existingUser),
                FakeTokenService(),
                IssueTokenPairService(FakeTokenService(), FakeRefreshTokenRepository()),
                twoHoursLater,
                meters,
            )

        assertFailsWith<InvalidTokenException> { lateUseCase.execute("valid-token") }
    }
}
