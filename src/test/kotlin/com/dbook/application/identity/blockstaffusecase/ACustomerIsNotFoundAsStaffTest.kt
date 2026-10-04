package com.dbook.application.identity.blockstaffusecase

import com.dbook.application.identity.BlockStaffCommand
import com.dbook.application.identity.staff.StaffUseCaseFixture
import com.dbook.domain.identity.UserNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ACustomerIsNotFoundAsStaffTest : StaffUseCaseFixture() {
    @Test
    fun `given a customer id when blocking as staff then it looks exactly like an unknown id`() {
        assertFailsWith<UserNotFoundException> { blockStaff.execute(BlockStaffCommand(actor, 3, "spam")) }
    }
}
