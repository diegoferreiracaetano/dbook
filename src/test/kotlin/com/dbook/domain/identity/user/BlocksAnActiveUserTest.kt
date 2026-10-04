package com.dbook.domain.identity.user

import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserStatus
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class BlocksAnActiveUserTest {
    @Test
    fun `given an active user when blocked then the status, reason and moment are set and the rest is kept`() {
        val at = Instant.parse("2026-10-04T12:00:00Z")
        val user = User(id = 7, email = "a@b.com", passwordHash = "h", name = "A", version = 3)

        val blocked = user.block("  chargeback fraud ", at)

        assertEquals(UserStatus.BLOCKED, blocked.status)
        assertEquals("chargeback fraud", blocked.blockedReason)
        assertEquals(at, blocked.blockedAt)
        assertEquals(7, blocked.id)
        assertEquals(3, blocked.version)
    }
}
