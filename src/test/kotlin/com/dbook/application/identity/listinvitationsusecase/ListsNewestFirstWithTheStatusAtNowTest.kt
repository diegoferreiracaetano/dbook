package com.dbook.application.identity.listinvitationsusecase

import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.identity.InvitationStatus
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.StaffInvitation
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class ListsNewestFirstWithTheStatusAtNowTest : StaffUseCaseFixture() {
    @Test
    fun `given a fresh and a stale invitation when listing then the newest is first and the stale one is EXPIRED`() {
        val stale = now.minus(Duration.ofHours(100))
        invitations.save(
            StaffInvitation(
                email = "old@example.com",
                role = Role.SUPPORT,
                tokenHash = "hash:old",
                invitedBy = 1,
                expiresAt = stale.plus(StaffInvitation.VALIDITY),
                createdAt = stale,
            ),
        )
        seedInvitation(email = "fresh@example.com", token = "fresh")

        val listed = listInvitations.execute()

        assertEquals(listOf("fresh@example.com", "old@example.com"), listed.map { it.invitation.email })
        assertEquals(listOf(InvitationStatus.PENDING, InvitationStatus.EXPIRED), listed.map { it.status })
    }
}
