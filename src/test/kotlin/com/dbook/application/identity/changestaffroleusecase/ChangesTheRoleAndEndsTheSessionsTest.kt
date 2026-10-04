package com.dbook.application.identity.changestaffroleusecase

import com.dbook.application.identity.ChangeStaffRoleCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class ChangesTheRoleAndEndsTheSessionsTest : StaffUseCaseFixture() {
    @Test
    fun `given a staff member when changing the role then the role changes and every session of theirs ends`() {
        val changed = changeRole.execute(ChangeStaffRoleCommand(actor, 2, Role.CATALOG_MANAGER))

        assertEquals(Role.CATALOG_MANAGER, changed.role)
        assertEquals(Role.CATALOG_MANAGER, users.findById(2)?.role)
        assertEquals(listOf(2L), refreshTokens.revokedForUsers)
    }
}
