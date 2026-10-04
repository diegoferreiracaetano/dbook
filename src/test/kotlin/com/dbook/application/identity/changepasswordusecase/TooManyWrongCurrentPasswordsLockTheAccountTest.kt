package com.dbook.application.identity.changepasswordusecase

import com.dbook.domain.identity.InvalidCredentialsException
import com.dbook.domain.identity.TooManyLoginAttemptsException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class TooManyWrongCurrentPasswordsLockTheAccountTest : ChangePasswordUseCaseFixture() {
    @Test
    fun `given three wrong attempts when trying the right password then it is locked like a login`() {
        repeat(3) { assertFailsWith<InvalidCredentialsException> { change(current = "wrong") } }

        assertFailsWith<TooManyLoginAttemptsException> { change() }
    }
}
