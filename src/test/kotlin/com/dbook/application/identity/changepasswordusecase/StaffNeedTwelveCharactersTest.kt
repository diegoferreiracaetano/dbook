package com.dbook.application.identity.changepasswordusecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StaffNeedTwelveCharactersTest : ChangePasswordUseCaseFixture() {
    @Test
    fun `given a staff member when the new password has eleven characters then it is refused`() {
        assertFailsWith<IllegalArgumentException> { change(new = "only-11-cha") }

        assertEquals("old", users.findById(1)?.passwordHash)
    }
}
