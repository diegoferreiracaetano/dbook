package com.dbook.application.identity.changestaffroleusecase

import com.dbook.application.identity.ChangeStaffRoleCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertTrue

class ChangingToTheSameRoleDoesNothingTest : StaffUseCaseFixture() {
    @Test
    fun `given the role the member already has when changing then nothing is revoked or audited`() {
        changeRole.execute(ChangeStaffRoleCommand(actor, 2, Role.SUPPORT))

        assertTrue(refreshTokens.revokedForUsers.isEmpty())
        assertTrue(audit.events.isEmpty())
    }
}
