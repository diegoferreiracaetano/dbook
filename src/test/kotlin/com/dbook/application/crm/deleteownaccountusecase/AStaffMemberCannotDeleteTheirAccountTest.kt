package com.dbook.application.crm.deleteownaccountusecase

import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.application.crm.DeleteOwnAccountCommand
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AStaffMemberCannotDeleteTheirAccountTest : CrmUseCaseFixture() {
    @Test
    fun `given a staff member when deleting their own account then it is refused`() {
        assertFailsWith<IllegalStateException> {
            deleteOwnAccount.execute(DeleteOwnAccountCommand(2, "current-password-1", "10.0.0.1"))
        }
    }
}
