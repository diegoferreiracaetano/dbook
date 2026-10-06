package com.dbook.application.identity.changestaffroleusecase

import com.dbook.application.identity.ChangeStaffRoleCommand
import com.dbook.application.identity.staff.InMemoryUserRepository
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class AllowsDemotingASuperAdminWhenAnotherRemainsTest : StaffUseCaseFixture() {
    override val users =
        InMemoryUserRepository(superAdmin, support, customer, staffUser(4, "second@example.com", Role.SUPER_ADMIN))

    @Test
    fun `given two active SUPER_ADMINs when demoting one then it works`() {
        changeRole.execute(ChangeStaffRoleCommand(Actor(99, Role.SUPER_ADMIN), 1, Role.SUPPORT))

        assertEquals(Role.SUPPORT, users.findById(1)?.role)
    }
}
