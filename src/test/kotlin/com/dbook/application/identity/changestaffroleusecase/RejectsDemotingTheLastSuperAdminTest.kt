package com.dbook.application.identity.changestaffroleusecase

import com.dbook.application.identity.ChangeStaffRoleCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RejectsDemotingTheLastSuperAdminTest : StaffUseCaseFixture() {
    @Test
    fun `given the only active SUPER_ADMIN when another actor demotes them then it is refused`() {
        assertFailsWith<IllegalStateException> {
            changeRole.execute(ChangeStaffRoleCommand(Actor(99, Role.SUPER_ADMIN), 1, Role.SUPPORT))
        }

        assertEquals(Role.SUPER_ADMIN, users.findById(1)?.role)
    }
}
