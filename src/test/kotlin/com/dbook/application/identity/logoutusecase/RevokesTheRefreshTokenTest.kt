package com.dbook.application.identity.logoutusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class RevokesTheRefreshTokenTest : LogoutUseCaseFixture() {
    @Test
    fun `given a live session when logging out then its refresh token is revoked`() {
        givenASessionWithToken("abc")

        useCase.execute("abc")

        assertEquals(true, isSpent("abc"))
    }
}
