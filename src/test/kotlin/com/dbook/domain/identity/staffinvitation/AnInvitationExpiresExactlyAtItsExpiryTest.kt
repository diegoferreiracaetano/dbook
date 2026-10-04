package com.dbook.domain.identity.staffinvitation

import com.dbook.domain.identity.InvitationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AnInvitationExpiresExactlyAtItsExpiryTest : StaffInvitationFixture() {
    @Test
    fun `given an invitation when read one second before and at its expiry then it is PENDING and then EXPIRED`() {
        val invitation = pending()

        assertEquals(InvitationStatus.PENDING, invitation.statusAt(invitation.expiresAt.minusSeconds(1)))
        assertEquals(InvitationStatus.EXPIRED, invitation.statusAt(invitation.expiresAt))
        assertFailsWith<IllegalStateException> { invitation.accept(invitation.expiresAt) }
    }
}
