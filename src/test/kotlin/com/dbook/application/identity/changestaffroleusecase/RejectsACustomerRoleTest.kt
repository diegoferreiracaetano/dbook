package com.dbook.application.identity.changestaffroleusecase

import com.dbook.application.identity.ChangeStaffRoleCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsACustomerRoleTest : StaffUseCaseFixture() {
    @Test
    fun `given the CLIENT role when changing a staff role then it is refused, the way out is blocking`() {
        assertFailsWith<IllegalArgumentException> { changeRole.execute(ChangeStaffRoleCommand(actor, 2, Role.CLIENT)) }
    }
}
