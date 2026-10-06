package com.dbook.application.identity.invitestaffusecase

import com.dbook.application.identity.InviteStaffCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StoresOnlyTheHashOfTheTokenTest : StaffUseCaseFixture() {
    @Test
    fun `given an invitation when it is stored then only the token hash is kept and it finds the invitation`() {
        committed { invite.execute(InviteStaffCommand(actor, "new@example.com", Role.SUPPORT)) }

        assertEquals("hash:token-1", invitations.findOpenByEmail("new@example.com")?.tokenHash)
        assertNull(invitations.findByTokenHash("token-1"))
    }
}
