package com.dbook.domain.identity.user

import com.dbook.domain.identity.User
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsBlockingWithoutAReasonTest {
    @Test
    fun `given an active user when blocked with a blank reason then it throws IllegalArgumentException`() {
        val user = User(email = "a@b.com", passwordHash = "h", name = "A")

        assertFailsWith<IllegalArgumentException> { user.block("   ", Instant.now()) }
    }
}
