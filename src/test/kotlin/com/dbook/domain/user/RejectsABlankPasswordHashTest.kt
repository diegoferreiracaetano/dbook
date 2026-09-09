package com.dbook.domain.user

import com.dbook.domain.User
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsABlankPasswordHashTest {
    @Test
    fun `given a blank passwordHash when a User is built then it throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            User(email = "diego@example.com", passwordHash = "")
        }
    }
}
