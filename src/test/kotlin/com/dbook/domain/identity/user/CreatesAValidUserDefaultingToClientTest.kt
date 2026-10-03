package com.dbook.domain.identity.user

import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import kotlin.test.Test
import kotlin.test.assertEquals

class CreatesAValidUserDefaultingToClientTest {
    @Test
    fun `given no role specified when a User is built then it defaults to CLIENT`() {
        val user = User(email = "diego@example.com", passwordHash = "hash", name = "Test User")

        assertEquals(Role.CLIENT, user.role)
    }
}
