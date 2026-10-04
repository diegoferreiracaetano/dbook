package com.dbook.application.identity.blockstaffusecase

import com.dbook.application.identity.BlockStaffCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsBlockingYourselfTest : StaffUseCaseFixture() {
    @Test
    fun `given the actor targeting themselves when blocking then it is refused`() {
        assertFailsWith<IllegalStateException> { blockStaff.execute(BlockStaffCommand(actor, 1, "oops")) }
    }
}
