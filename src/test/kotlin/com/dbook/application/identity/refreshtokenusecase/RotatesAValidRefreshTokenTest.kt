package com.dbook.application.identity.refreshtokenusecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RotatesAValidRefreshTokenTest : RefreshTokenUseCaseFixture() {
    @Test
    fun `given a valid refresh token when refreshing then a new pair is issued and the old token is spent`() {
        val pair = refresh()

        assertEquals("access-1", pair.accessToken)
        assertTrue(tokenIsSpent())
        assertEquals(2, refreshTokens.saved.size)
    }
}
