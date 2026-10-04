package com.dbook.domain.identity.staffinvitation

import com.dbook.domain.identity.Role
import com.dbook.domain.identity.StaffInvitation
import java.time.Instant

abstract class StaffInvitationFixture {
    protected val now: Instant = Instant.parse("2026-10-04T12:00:00Z")

    protected fun pending(email: String = "maria@example.com") =
        StaffInvitation.issue(email, Role.SUPPORT, tokenHash = "hash", invitedBy = 1, now = now)
}
