package com.dbook.application.identity.invitestaffusecase

import com.dbook.application.identity.InviteStaffCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.identity.EmailDeliveryException
import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertNotNull

class AFailedDeliveryDoesNotUndoTheInvitationTest : StaffUseCaseFixture() {
    @Test
    fun `given a mail server that is down when inviting then the invitation still exists to be sent again`() {
        emails.failWith = EmailDeliveryException("smtp down")

        committed { invite.execute(InviteStaffCommand(actor, "new@example.com", Role.SUPPORT)) }

        assertNotNull(invitations.findOpenByEmail("new@example.com"))
    }
}
