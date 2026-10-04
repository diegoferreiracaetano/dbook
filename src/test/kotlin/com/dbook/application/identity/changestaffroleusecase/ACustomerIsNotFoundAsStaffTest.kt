package com.dbook.application.identity.changestaffroleusecase

import com.dbook.application.identity.ChangeStaffRoleCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.UserNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ACustomerIsNotFoundAsStaffTest : StaffUseCaseFixture() {
    @Test
    fun `given a customer id when changing the role then it looks exactly like an unknown id`() {
        assertFailsWith<UserNotFoundException> { changeRole.execute(ChangeStaffRoleCommand(actor, 3, Role.SUPPORT)) }
    }
}
