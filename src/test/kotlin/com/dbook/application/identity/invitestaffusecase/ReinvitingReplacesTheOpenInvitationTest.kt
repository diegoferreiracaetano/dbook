package com.dbook.application.identity.invitestaffusecase

import com.dbook.application.identity.InviteStaffCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.InvitationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ReinvitingReplacesTheOpenInvitationTest : StaffUseCaseFixture() {
    @Test
    fun `given an open invitation when inviting the address again then the old one is revoked`() {
        val first = committed { invite.execute(InviteStaffCommand(actor, "new@example.com", Role.SUPPORT)) }
        val second = committed { invite.execute(InviteStaffCommand(actor, "new@example.com", Role.SUPPORT)) }

        assertEquals(InvitationStatus.REVOKED, invitations.findById(requireNotNull(first.invitation.id))?.statusAt(now))
        assertEquals(second.invitation, invitations.findOpenByEmail("new@example.com"))
        assertNotEquals(first.invitation.tokenHash, second.invitation.tokenHash)
    }
}
