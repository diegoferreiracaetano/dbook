package com.dbook.infrastructure.persistence.identity

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

// The flush in save() is what makes this legal: the revocation must reach the database before the new row,
// or the partial unique index (one open invitation per e-mail) rejects the insert.
class ReInvitingRevokesTheOpenInvitationAndCreatesAnotherTest : StaffInvitationRepositoryAdapterFixture() {
    @Test
    fun `given an open invitation when it is revoked and another is created then only the new one is open`() {
        val inviter = inviterId()
        val email = uniqueEmail()
        val first = invitations.save(issue(email, inviter))

        invitations.save(first.revoke(Instant.now()))
        val second = invitations.save(issue(email, inviter))

        assertEquals(second.id, invitations.findOpenByEmail(email)?.id)
    }
}
