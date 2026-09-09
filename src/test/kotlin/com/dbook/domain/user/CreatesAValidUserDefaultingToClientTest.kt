package com.dbook.domain.user

import com.dbook.domain.Role
import com.dbook.domain.User
import kotlin.test.Test
import kotlin.test.assertEquals

class CreatesAValidUserDefaultingToClientTest {
    @Test
    fun `given no role specified when a User is built then it defaults to CLIENT`() {
        val user = User(email = "diego@example.com", passwordHash = "hash")

        assertEquals(Role.CLIENT, user.role)
    }
}
