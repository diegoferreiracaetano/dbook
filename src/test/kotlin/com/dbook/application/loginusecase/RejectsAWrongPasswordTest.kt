package com.dbook.application.loginusecase

import com.dbook.application.LoginCommand
import com.dbook.domain.InvalidCredentialsException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsAWrongPasswordTest : LoginUseCaseFixture() {
    @Test
    fun `given a wrong password when logging in then it throws InvalidCredentialsException`() {
        assertFailsWith<InvalidCredentialsException> {
            useCase.execute(LoginCommand(email = "diego@example.com", password = "wrong-password"))
        }
    }
}
