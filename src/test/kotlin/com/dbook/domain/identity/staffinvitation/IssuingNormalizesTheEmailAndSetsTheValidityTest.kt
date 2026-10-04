package com.dbook.domain.identity.staffinvitation

import com.dbook.domain.identity.InvitationStatus
import com.dbook.domain.identity.StaffInvitation
import kotlin.test.Test
import kotlin.test.assertEquals

class IssuingNormalizesTheEmailAndSetsTheValidityTest : StaffInvitationFixture() {
    @Test
    fun `given an e-mail with capitals and spaces when issued then it is trimmed, lowercased and valid for 72 hours`() {
        val invitation = pending(email = "  Maria@Example.COM ")

        assertEquals("maria@example.com", invitation.email)
        assertEquals(now.plus(StaffInvitation.VALIDITY), invitation.expiresAt)
        assertEquals(InvitationStatus.PENDING, invitation.statusAt(now))
    }
}
