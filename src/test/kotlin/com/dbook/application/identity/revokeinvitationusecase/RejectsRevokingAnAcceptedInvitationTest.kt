package com.dbook.application.identity.revokeinvitationusecase

import com.dbook.application.identity.RevokeInvitationCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsRevokingAnAcceptedInvitationTest : StaffUseCaseFixture() {
    @Test
    fun `given an accepted invitation when revoking then it is refused`() {
        val accepted = invitations.save(seedInvitation().accept(now))

        assertFailsWith<IllegalStateException> {
            revoke.execute(
                RevokeInvitationCommand(actor, requireNotNull(accepted.id)),
            )
        }
    }
}
