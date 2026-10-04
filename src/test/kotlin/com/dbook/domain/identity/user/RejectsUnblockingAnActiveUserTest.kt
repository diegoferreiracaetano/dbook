package com.dbook.domain.identity.user

import com.dbook.domain.identity.User
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsUnblockingAnActiveUserTest {
    @Test
    fun `given an active user when unblocked then it throws IllegalStateException`() {
        assertFailsWith<IllegalStateException> { User(email = "a@b.com", passwordHash = "h", name = "A").unblock() }
    }
}
