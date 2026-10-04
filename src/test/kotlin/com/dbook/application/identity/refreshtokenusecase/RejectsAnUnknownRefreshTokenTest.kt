package com.dbook.application.identity.refreshtokenusecase

import com.dbook.domain.identity.InvalidTokenException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsAnUnknownRefreshTokenTest : RefreshTokenUseCaseFixture() {
    @Test
    fun `given a token that was never issued when refreshing then it throws InvalidTokenException`() {
        assertFailsWith<InvalidTokenException> { refresh(token = "never-issued") }
    }
}
