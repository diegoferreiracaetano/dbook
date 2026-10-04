package com.dbook.application.identity.loginusecase

import com.dbook.domain.identity.InvalidCredentialsException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsAnUnknownEmailTest : LoginUseCaseFixture() {
    @Test
    fun `given an unknown email when logging in then it throws InvalidCredentialsException`() {
        assertFailsWith<InvalidCredentialsException> { login(email = "nobody@example.com") }
    }
}
