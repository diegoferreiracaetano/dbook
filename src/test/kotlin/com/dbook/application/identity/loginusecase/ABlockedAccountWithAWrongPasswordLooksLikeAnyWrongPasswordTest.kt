package com.dbook.application.identity.loginusecase

import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.InvalidCredentialsException
import com.dbook.domain.identity.User
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ABlockedAccountWithAWrongPasswordLooksLikeAnyWrongPasswordTest : LoginUseCaseFixture() {
    override val existingUser =
        User(id = 1, email = "diego@example.com", passwordHash = "x", name = "Diego", role = Role.CLIENT)
            .block("chargeback fraud", now)

    @Test
    fun `given a blocked account when logging in with a wrong password then it is an ordinary invalid credential`() {
        assertFailsWith<InvalidCredentialsException> { login(password = "wrong-password") }
    }
}
