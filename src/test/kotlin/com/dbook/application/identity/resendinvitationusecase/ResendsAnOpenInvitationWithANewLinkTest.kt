package com.dbook.application.identity.resendinvitationusecase

import com.dbook.application.identity.ResendInvitationCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.identity.InvitationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResendsAnOpenInvitationWithANewLinkTest : StaffUseCaseFixture() {
    @Test
    fun `given an open invitation when resending then the old link dies, a new one is mailed and the audit has it`() {
        val seeded = seedInvitation(token = "old")

        committed { resend.execute(ResendInvitationCommand(actor, requireNotNull(seeded.id))) }

        assertNull(invitations.findByTokenHash("hash:old"))
        assertEquals(InvitationStatus.PENDING, invitations.findByTokenHash("hash:token-1")?.statusAt(now))
        assertTrue(emails.sent.single().body.contains("token=token-1"))
        assertEquals(AuditAction.STAFF_INVITATION_RESENT, audit.events.single().action)
    }
}
