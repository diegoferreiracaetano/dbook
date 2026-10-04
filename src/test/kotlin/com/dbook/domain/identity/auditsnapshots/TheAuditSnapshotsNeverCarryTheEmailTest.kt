package com.dbook.domain.identity.auditsnapshots

import com.dbook.domain.identity.Role
import com.dbook.domain.identity.StaffInvitation
import com.dbook.domain.identity.User
import com.dbook.domain.identity.toAuditSnapshot
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

// audit_log is append-only, so personal data written there could never be erased: ids, role and status only.
class TheAuditSnapshotsNeverCarryTheEmailTest {
    @Test
    fun `given a user and an invitation when their audit snapshots are taken then there is no e-mail, hash or token`() {
        val user = User(id = 3, email = "maria@example.com", passwordHash = "secret-hash", name = "Maria")
        val invitation =
            StaffInvitation.issue(
                "joao@example.com",
                Role.SUPPORT,
                tokenHash = "token-hash",
                invitedBy = 1,
                now = Instant.now(),
            )

        assertEquals(setOf("id", "role", "status"), user.toAuditSnapshot().keys)
        assertEquals(
            setOf("id", "role", "invitedBy", "expiresAt", "acceptedAt", "revokedAt"),
            invitation.toAuditSnapshot().keys,
        )
    }
}
