package com.dbook.application.identity.acceptinvitationusecase

import com.dbook.application.identity.AcceptInvitationCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.InvalidInvitationException
import com.dbook.domain.identity.StaffInvitation
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RefusesAnExpiredInvitationTest : StaffUseCaseFixture() {
    @Test
    fun `given an expired invitation when accepting then it is the uniform invalid invitation error`() {
        val created = now.minus(StaffInvitation.VALIDITY).minusSeconds(1)
        invitations.save(
            StaffInvitation(
                email = "new@example.com",
                role = Role.SUPPORT,
                tokenHash = "hash:abc",
                invitedBy = 1,
                expiresAt = created.plus(StaffInvitation.VALIDITY),
                createdAt = created,
            ),
        )

        assertFailsWith<InvalidInvitationException> {
            accept.execute(AcceptInvitationCommand("abc", "Maria", "a-long-passphrase-1"))
        }
    }
}
