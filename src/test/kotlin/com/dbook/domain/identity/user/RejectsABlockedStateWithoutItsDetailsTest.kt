package com.dbook.domain.identity.user

import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserStatus
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsABlockedStateWithoutItsDetailsTest {
    @Test
    fun `given a BLOCKED status without reason or moment when building the user then it throws`() {
        assertFailsWith<IllegalArgumentException> {
            User(email = "a@b.com", passwordHash = "h", name = "A", status = UserStatus.BLOCKED)
        }
    }
}
