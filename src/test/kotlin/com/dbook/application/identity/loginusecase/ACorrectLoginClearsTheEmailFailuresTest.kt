package com.dbook.application.identity.loginusecase

import com.dbook.domain.identity.InvalidCredentialsException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ACorrectLoginClearsTheEmailFailuresTest : LoginUseCaseFixture() {
    @Test
    fun `given two failures when the user then logs in correctly then the email starts from zero again`() {
        repeat(2) { assertFailsWith<InvalidCredentialsException> { login(password = "wrong-password") } }

        login()

        assertEquals(0, limiter.failuresOf("email:diego@example.com"))
    }
}
