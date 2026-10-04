package com.dbook.application.identity.acceptinvitationusecase

import com.dbook.application.identity.AcceptInvitationCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.identity.InvitationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AWeakPasswordLeavesTheInvitationUsableTest : StaffUseCaseFixture() {
    @Test
    fun `given a weak password when accepting then it is refused and the invitation stays pending`() {
        val seeded = seedInvitation(token = "abc")

        assertFailsWith<IllegalArgumentException> {
            accept.execute(
                AcceptInvitationCommand("abc", "Maria", "short-11-ch"),
            )
        }

        assertEquals(InvitationStatus.PENDING, invitations.findById(requireNotNull(seeded.id))?.statusAt(now))
        assertNull(users.findByEmail("new@example.com"))
    }
}
