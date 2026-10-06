package com.dbook.application.identity.invitestaffusecase

import com.dbook.application.identity.InviteStaffCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.InvitationStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class IssuesAPendingInvitationWithANormalizedEmailTest : StaffUseCaseFixture() {
    @Test
    fun `given a new address when inviting then a pending invitation exists and the email is normalized`() {
        val view = committed { invite.execute(InviteStaffCommand(actor, "  New@Example.COM ", Role.CATALOG_MANAGER)) }

        assertEquals(InvitationStatus.PENDING, view.status)
        assertEquals("new@example.com", view.invitation.email)
        assertEquals(Role.CATALOG_MANAGER, view.invitation.role)
        assertEquals(view.invitation, invitations.findOpenByEmail("new@example.com"))
    }
}
