package com.dbook.application.identity.revokeinvitationusecase

import com.dbook.application.identity.RevokeInvitationCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.audit.AuditAction
import com.dbook.domain.identity.InvitationStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class RevokesAnOpenInvitationTest : StaffUseCaseFixture() {
    @Test
    fun `given an open invitation when revoking then its link stops working and the audit has it`() {
        val seeded = seedInvitation()

        revoke.execute(RevokeInvitationCommand(actor, requireNotNull(seeded.id)))

        assertEquals(InvitationStatus.REVOKED, invitations.findById(requireNotNull(seeded.id))?.statusAt(now))
        assertEquals(AuditAction.STAFF_INVITATION_REVOKED, audit.events.single().action)
    }
}
