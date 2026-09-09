package com.dbook.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserTest {
    @Test
    fun `creates a valid user, defaulting to CLIENT`() {
        val user = User(email = "diego@example.com", passwordHash = "hash")
        assertEquals(Role.CLIENT, user.role)
    }

    @Test
    fun `rejects a blank email`() {
        assertFailsWith<IllegalArgumentException> {
            User(email = "", passwordHash = "hash")
        }
    }

    @Test
    fun `rejects an email without an @`() {
        assertFailsWith<IllegalArgumentException> {
            User(email = "not-an-email", passwordHash = "hash")
        }
    }

    @Test
    fun `rejects a blank passwordHash`() {
        assertFailsWith<IllegalArgumentException> {
            User(email = "diego@example.com", passwordHash = "")
        }
    }
}
