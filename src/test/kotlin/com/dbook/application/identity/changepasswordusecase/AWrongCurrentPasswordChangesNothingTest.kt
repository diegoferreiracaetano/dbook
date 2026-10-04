package com.dbook.application.identity.changepasswordusecase

import com.dbook.domain.identity.InvalidCredentialsException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AWrongCurrentPasswordChangesNothingTest : ChangePasswordUseCaseFixture() {
    @Test
    fun `given a wrong current password when changing then it is refused and nothing changes`() {
        assertFailsWith<InvalidCredentialsException> { change(current = "wrong") }

        assertEquals("old", users.findById(1)?.passwordHash)
        assertTrue(refreshTokens.revokedForUsers.isEmpty())
    }
}
