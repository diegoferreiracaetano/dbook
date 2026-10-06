package com.dbook.application.identity.changestaffroleusecase

import com.dbook.application.identity.ChangeStaffRoleCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsChangingYourOwnRoleTest : StaffUseCaseFixture() {
    @Test
    fun `given the actor targeting themselves when changing the role then it is refused`() {
        assertFailsWith<IllegalStateException> { changeRole.execute(ChangeStaffRoleCommand(actor, 1, Role.SUPPORT)) }
    }
}
