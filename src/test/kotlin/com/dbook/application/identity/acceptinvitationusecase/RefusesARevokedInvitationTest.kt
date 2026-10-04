package com.dbook.application.identity.acceptinvitationusecase

import com.dbook.application.identity.AcceptInvitationCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.identity.InvalidInvitationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RefusesARevokedInvitationTest : StaffUseCaseFixture() {
    @Test
    fun `given a revoked invitation when accepting then it is the uniform invalid invitation error`() {
        invitations.save(seedInvitation(token = "abc").revoke(now))

        assertFailsWith<InvalidInvitationException> {
            accept.execute(AcceptInvitationCommand("abc", "Maria", "a-long-passphrase-1"))
        }
    }
}
