package com.dbook.application.identity.invitestaffusecase

import com.dbook.application.identity.InviteStaffCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.audit.AuditAction
import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RecordsTheInvitationInTheAuditLogTest : StaffUseCaseFixture() {
    @Test
    fun `given an invitation when it is issued then the audit trail has it without the invitee email`() {
        val view = committed { invite.execute(InviteStaffCommand(actor, "new@example.com", Role.SUPPORT)) }

        val event = audit.events.single()
        assertEquals(AuditAction.STAFF_INVITED, event.action)
        assertEquals(actor, event.actor)
        assertEquals(view.invitation.id.toString(), event.targetId)
        assertFalse(event.after.toString().contains("new@example.com"))
    }
}
