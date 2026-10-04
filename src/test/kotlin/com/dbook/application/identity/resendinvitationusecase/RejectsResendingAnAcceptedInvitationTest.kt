package com.dbook.application.identity.resendinvitationusecase

import com.dbook.application.identity.ResendInvitationCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RejectsResendingAnAcceptedInvitationTest : StaffUseCaseFixture() {
    @Test
    fun `given an accepted invitation when resending then it is refused and nothing is mailed`() {
        val accepted = invitations.save(seedInvitation().accept(now))

        assertFailsWith<IllegalStateException> {
            committed { resend.execute(ResendInvitationCommand(actor, requireNotNull(accepted.id))) }
        }

        assertTrue(emails.sent.isEmpty())
    }
}
