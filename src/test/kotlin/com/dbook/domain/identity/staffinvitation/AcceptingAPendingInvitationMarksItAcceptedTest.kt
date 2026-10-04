package com.dbook.domain.identity.staffinvitation

import com.dbook.domain.identity.InvitationStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class AcceptingAPendingInvitationMarksItAcceptedTest : StaffInvitationFixture() {
    @Test
    fun `given a pending invitation when accepted then it is ACCEPTED at that moment`() {
        val accepted = pending().accept(now.plusSeconds(60))

        assertEquals(InvitationStatus.ACCEPTED, accepted.statusAt(now.plusSeconds(120)))
        assertEquals(now.plusSeconds(60), accepted.acceptedAt)
    }
}
