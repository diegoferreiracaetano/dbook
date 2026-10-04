package com.dbook.domain.identity.staffinvitation

import com.dbook.domain.identity.Role
import com.dbook.domain.identity.StaffInvitation
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ARoleWithoutPortalAccessCannotBeInvitedTest : StaffInvitationFixture() {
    @Test
    fun `given the CLIENT role when issuing an invitation then it throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            StaffInvitation.issue("maria@example.com", Role.CLIENT, tokenHash = "hash", invitedBy = 1, now = now)
        }
    }
}
