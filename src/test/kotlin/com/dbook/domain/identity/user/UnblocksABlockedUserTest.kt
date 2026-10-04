package com.dbook.domain.identity.user

import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserStatus
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UnblocksABlockedUserTest {
    @Test
    fun `given a blocked user when unblocked then it is active and the block details are gone`() {
        val blocked = User(email = "a@b.com", passwordHash = "h", name = "A").block("spam", Instant.now())

        val active = blocked.unblock()

        assertEquals(UserStatus.ACTIVE, active.status)
        assertNull(active.blockedReason)
        assertNull(active.blockedAt)
    }
}
