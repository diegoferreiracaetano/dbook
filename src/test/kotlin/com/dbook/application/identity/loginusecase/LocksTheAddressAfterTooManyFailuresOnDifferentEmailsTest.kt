package com.dbook.application.identity.loginusecase

import com.dbook.domain.identity.InvalidCredentialsException
import com.dbook.domain.identity.TooManyLoginAttemptsException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class LocksTheAddressAfterTooManyFailuresOnDifferentEmailsTest : LoginUseCaseFixture() {
    @Test
    fun `given five failures from an address on different emails when trying again then the address is locked`() {
        repeat(5) { attempt ->
            assertFailsWith<InvalidCredentialsException> { login(email = "target$attempt@example.com") }
        }

        assertFailsWith<TooManyLoginAttemptsException> { login(email = "another@example.com") }
    }
}
