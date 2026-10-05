package com.dbook.application.crm.blockcustomerusecase

import com.dbook.application.crm.BlockCustomerCommand
import com.dbook.application.crm.CrmUseCaseFixture
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class RejectsAReasonUnderTenCharactersTest : CrmUseCaseFixture() {
    @Test
    fun `given a nine character reason when blocking then it is refused and the customer stays active`() {
        assertFailsWith<IllegalArgumentException> {
            blockCustomer.execute(BlockCustomerCommand(support, customerId, "  too short "))
        }

        assertFalse(users.findById(customerId)?.isBlocked == true)
    }
}
