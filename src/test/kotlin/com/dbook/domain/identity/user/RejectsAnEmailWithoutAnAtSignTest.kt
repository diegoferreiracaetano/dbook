package com.dbook.domain.identity.user

import com.dbook.domain.identity.User
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsAnEmailWithoutAnAtSignTest {
    @Test
    fun `given an email without an at sign when a User is built then it throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            User(email = "not-an-email", passwordHash = "hash", name = "Test User")
        }
    }
}
