package com.dbook.application.identity.loginusecase

import com.dbook.domain.identity.InvalidCredentialsException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsAWrongPasswordTest : LoginUseCaseFixture() {
    @Test
    fun `given a wrong password when logging in then it throws InvalidCredentialsException`() {
        assertFailsWith<InvalidCredentialsException> { login(password = "wrong-password") }
    }
}
