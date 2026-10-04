package com.dbook.application.identity.blockstaffusecase

import com.dbook.application.identity.BlockStaffCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsBlockingTheLastSuperAdminTest : StaffUseCaseFixture() {
    @Test
    fun `given the only active SUPER_ADMIN when another actor blocks them then it is refused`() {
        assertFailsWith<IllegalStateException> {
            blockStaff.execute(BlockStaffCommand(Actor(99, Role.SUPER_ADMIN), 1, "oops"))
        }
    }
}
