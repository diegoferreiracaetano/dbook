package com.dbook.domain.identity.user

import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith

class OnlyAnUnsavedOrAnAlreadyAnonymizedOrStaffAccountIsRefusedTest {
    @Test
    fun `given a staff member, an anonymized account or an unsaved user when anonymizing then it is refused`() {
        val now = Instant.now()

        assertFailsWith<IllegalStateException> {
            User(id = 1, email = "s@example.com", passwordHash = "h", name = "S", role = Role.SUPPORT).anonymize(now)
        }
        assertFailsWith<IllegalStateException> {
            User(id = 2, email = "c@example.com", passwordHash = "h", name = "C").anonymize(now).anonymize(now)
        }
        assertFailsWith<IllegalArgumentException> {
            User(email = "new@example.com", passwordHash = "h", name = "N").anonymize(now)
        }
    }
}
