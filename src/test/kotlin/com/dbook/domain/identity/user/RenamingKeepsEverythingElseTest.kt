package com.dbook.domain.identity.user

import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.User
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RenamingKeepsEverythingElseTest {
    @Test
    fun `given a blocked staff user when renamed then the block, the role and the version are kept`() {
        val blocked =
            User(id = 4, email = "a@b.com", passwordHash = "h", name = "A", role = Role.SUPPORT, version = 9)
                .block("spam", Instant.now())

        val renamed = blocked.rename("New Name")

        assertEquals("New Name", renamed.name)
        assertTrue(renamed.isBlocked)
        assertEquals(Role.SUPPORT, renamed.role)
        assertEquals(9, renamed.version)
    }
}
