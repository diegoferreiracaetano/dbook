package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.DuplicateOpenInvitationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ASecondOpenInvitationForTheSameEmailIsRefusedTest : StaffInvitationRepositoryAdapterFixture() {
    @Test
    fun `given an open invitation when another is saved for its e-mail then it throws a duplicate error`() {
        val inviter = inviterId()
        val email = uniqueEmail()
        invitations.save(issue(email, inviter))

        assertFailsWith<DuplicateOpenInvitationException> { invitations.save(issue(email, inviter)) }
    }
}
