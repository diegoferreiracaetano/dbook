package com.dbook.domain.identity.staffinvitation

import com.dbook.domain.identity.InvitationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

// An expired invitation still holds the e-mail's single open slot, so it has to be closable and renewable.
class AnExpiredInvitationCanStillBeRevokedAndReissuedTest : StaffInvitationFixture() {
    @Test
    fun `given an expired invitation when revoked or reissued then both work and reissuing renews token and expiry`() {
        val expired = pending()
        val later = expired.expiresAt.plusSeconds(3600)

        assertFalse(expired.revoke(later).isOpen)
        val reissued = expired.reissue("new-hash", later)

        assertEquals("new-hash", reissued.tokenHash)
        assertEquals(InvitationStatus.PENDING, reissued.statusAt(later))
        assertEquals(expired.email, reissued.email)
    }
}
