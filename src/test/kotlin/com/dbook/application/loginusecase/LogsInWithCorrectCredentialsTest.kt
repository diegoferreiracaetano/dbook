package com.dbook.application.loginusecase

import com.dbook.application.LoginCommand
import kotlin.test.Test
import kotlin.test.assertEquals

class LogsInWithCorrectCredentialsTest : LoginUseCaseFixture() {
    @Test
    fun `given correct credentials when logging in then tokens are issued and the refresh hash is saved`() {
        val tokens = useCase.execute(LoginCommand(email = "diego@example.com", password = "correct-password"))

        assertEquals("access-1", tokens.accessToken)
        assertEquals(1, refreshTokenRepository.saved.size)
        assertEquals("hash:${tokens.refreshToken}", refreshTokenRepository.saved.first().tokenHash)
    }
}
