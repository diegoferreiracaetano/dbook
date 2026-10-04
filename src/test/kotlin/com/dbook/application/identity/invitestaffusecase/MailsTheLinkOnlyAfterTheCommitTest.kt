package com.dbook.application.identity.invitestaffusecase

import com.dbook.application.identity.InviteStaffCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MailsTheLinkOnlyAfterTheCommitTest : StaffUseCaseFixture() {
    @Test
    fun `given a rolled back transaction when inviting then no email goes out`() {
        rolledBack { invite.execute(InviteStaffCommand(actor, "new@example.com", Role.SUPPORT)) }

        assertTrue(emails.sent.isEmpty())
    }

    @Test
    fun `given a committed transaction when inviting then the link with the token is mailed to the invitee`() {
        committed { invite.execute(InviteStaffCommand(actor, "new@example.com", Role.SUPPORT)) }

        assertEquals(1, emails.sent.size)
        assertEquals("new@example.com", emails.sent.single().to)
        assertTrue(emails.sent.single().body.contains("http://portal.test/accept-invite?token=token-1"))
    }
}
