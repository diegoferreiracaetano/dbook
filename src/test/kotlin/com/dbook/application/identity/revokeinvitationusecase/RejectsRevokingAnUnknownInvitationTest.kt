package com.dbook.application.identity.revokeinvitationusecase

import com.dbook.application.identity.RevokeInvitationCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.identity.InvitationNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsRevokingAnUnknownInvitationTest : StaffUseCaseFixture() {
    @Test
    fun `given an unknown id when revoking then the invitation is not found`() {
        assertFailsWith<InvitationNotFoundException> { revoke.execute(RevokeInvitationCommand(actor, 99)) }
    }
}
