package com.dbook.application.identity.resendinvitationusecase

import com.dbook.application.identity.ResendInvitationCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.identity.InvitationNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsResendingAnUnknownInvitationTest : StaffUseCaseFixture() {
    @Test
    fun `given an unknown id when resending then the invitation is not found`() {
        assertFailsWith<InvitationNotFoundException> {
            committed { resend.execute(ResendInvitationCommand(actor, 99)) }
        }
    }
}
