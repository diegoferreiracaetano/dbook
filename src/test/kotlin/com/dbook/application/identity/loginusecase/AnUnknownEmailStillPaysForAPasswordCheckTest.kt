package com.dbook.application.identity.loginusecase

import com.dbook.domain.identity.InvalidCredentialsException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AnUnknownEmailStillPaysForAPasswordCheckTest : LoginUseCaseFixture() {
    @Test
    fun `given an unknown email when logging in then a password check still runs`() {
        assertFailsWith<InvalidCredentialsException> { login(email = "nobody@example.com") }

        assertEquals(1, hasher.matchCalls)
    }
}
