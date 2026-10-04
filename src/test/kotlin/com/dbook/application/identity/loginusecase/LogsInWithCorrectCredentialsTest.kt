package com.dbook.application.identity.loginusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class LogsInWithCorrectCredentialsTest : LoginUseCaseFixture() {
    @Test
    fun `given correct credentials when logging in then tokens are issued and the refresh hash is saved`() {
        val tokens = login()

        assertEquals("access-1", tokens.accessToken)
        assertEquals(1, refreshTokenRepository.saved.size)
        assertEquals("hash:${tokens.refreshToken}", refreshTokenRepository.saved.first().tokenHash)
    }
}
