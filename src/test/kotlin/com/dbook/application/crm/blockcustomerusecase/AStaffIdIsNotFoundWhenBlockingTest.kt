package com.dbook.application.crm.blockcustomerusecase

import com.dbook.application.crm.BlockCustomerCommand
import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.domain.identity.UserNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AStaffIdIsNotFoundWhenBlockingTest : CrmUseCaseFixture() {
    @Test
    fun `given the id of a staff member when blocking through the CRM then it is not found`() {
        assertFailsWith<UserNotFoundException> {
            blockCustomer.execute(BlockCustomerCommand(support, 4, "a reason that is long enough"))
        }
    }
}
