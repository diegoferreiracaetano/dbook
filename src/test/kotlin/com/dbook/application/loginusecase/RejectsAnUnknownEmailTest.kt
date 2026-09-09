package com.dbook.application.loginusecase

import com.dbook.application.LoginCommand
import com.dbook.domain.InvalidCredentialsException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsAnUnknownEmailTest : LoginUseCaseFixture() {
    @Test
    fun `given an unknown email when logging in then it throws InvalidCredentialsException`() {
        assertFailsWith<InvalidCredentialsException> {
            useCase.execute(LoginCommand(email = "nobody@example.com", password = "correct-password"))
        }
    }
}
