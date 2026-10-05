package com.dbook.application.crm.blockcustomerusecase

import com.dbook.application.crm.BlockCustomerCommand
import com.dbook.application.crm.CrmUseCaseFixture
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BlockingAnAlreadyBlockedCustomerIsRefusedTest : CrmUseCaseFixture() {
    @Test
    fun `given a blocked customer when blocking again then it is refused`() {
        blockCustomer.execute(BlockCustomerCommand(support, customerId, "a reason that is long enough"))

        assertFailsWith<IllegalStateException> {
            blockCustomer.execute(BlockCustomerCommand(support, customerId, "a reason that is long enough"))
        }
    }
}
