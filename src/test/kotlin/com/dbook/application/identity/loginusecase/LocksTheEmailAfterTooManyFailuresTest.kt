package com.dbook.application.identity.loginusecase

import com.dbook.domain.identity.InvalidCredentialsException
import com.dbook.domain.identity.TooManyLoginAttemptsException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocksTheEmailAfterTooManyFailuresTest : LoginUseCaseFixture() {
    @Test
    fun `given three failures for an email when trying again then it is locked even with the right password`() {
        repeat(3) { assertFailsWith<InvalidCredentialsException> { login(password = "wrong-password") } }

        val locked = assertFailsWith<TooManyLoginAttemptsException> { login() }

        assertEquals(FakeLoginAttemptLimiter.RETRY_AFTER_SECONDS, locked.retryAfterSeconds)
        assertEquals(1.0, count("rate_limited"))
    }
}
