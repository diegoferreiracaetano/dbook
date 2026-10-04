package com.dbook.application.identity.loginusecase

import com.dbook.domain.identity.AccountBlockedException
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RejectsABlockedAccountAfterCorrectCredentialsTest : LoginUseCaseFixture() {
    override val existingUser =
        User(id = 1, email = "diego@example.com", passwordHash = "x", name = "Diego", role = Role.CLIENT)
            .block("chargeback fraud", now)

    @Test
    fun `given a blocked account when logging in with the right password then it throws AccountBlockedException`() {
        assertFailsWith<AccountBlockedException> { login() }

        assertTrue(refreshTokenRepository.saved.isEmpty())
        assertEquals(1.0, count("blocked"))
    }
}
