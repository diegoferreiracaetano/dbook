package com.dbook.domain.user

import com.dbook.domain.User
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsABlankEmailTest {
    @Test
    fun `given a blank email when a User is built then it throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            User(email = "", passwordHash = "hash")
        }
    }
}
