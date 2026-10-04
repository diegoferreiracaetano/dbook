package com.dbook.domain.identity.user

import com.dbook.domain.identity.User
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsBlockingAnAlreadyBlockedUserTest {
    @Test
    fun `given a blocked user when blocked again then it throws IllegalStateException`() {
        val blocked = User(email = "a@b.com", passwordHash = "h", name = "A").block("spam", Instant.now())

        assertFailsWith<IllegalStateException> { blocked.block("spam again", Instant.now()) }
    }
}
