package com.dbook.application.identity.acceptinvitationusecase

import com.dbook.application.identity.AcceptInvitationCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.common.audit.AuditAction
import kotlin.test.Test
import kotlin.test.assertEquals

class TheAuditTrailNamesTheNewAccountAsTheActorTest : StaffUseCaseFixture() {
    @Test
    fun `given an accepted invitation when the audit is read then the actor is the account it created`() {
        seedInvitation(token = "abc")

        val user = accept.execute(AcceptInvitationCommand("abc", "Maria", "a-long-passphrase-1"))

        val event = audit.events.single()
        assertEquals(AuditAction.STAFF_INVITATION_ACCEPTED, event.action)
        assertEquals(user.id, event.actor.id)
    }
}
