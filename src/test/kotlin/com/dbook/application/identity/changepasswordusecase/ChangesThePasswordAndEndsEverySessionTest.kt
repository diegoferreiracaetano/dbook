package com.dbook.application.identity.changepasswordusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class ChangesThePasswordAndEndsEverySessionTest : ChangePasswordUseCaseFixture() {
    @Test
    fun `given the right current password when changing then the hash changes and every session ends`() {
        change()

        assertEquals("hashed:a-brand-new-passphrase", users.findById(1)?.passwordHash)
        assertEquals(listOf(1L), refreshTokens.revokedForUsers)
    }
}
