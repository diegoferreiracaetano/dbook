package com.dbook.application.crm.unblockcustomerusecase

import com.dbook.application.crm.BlockCustomerCommand
import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.application.crm.UnblockCustomerCommand
import com.dbook.domain.common.audit.AuditAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class UnblocksACustomerAndAuditsTest : CrmUseCaseFixture() {
    @Test
    fun `given a blocked customer when unblocking then they are active again and the audit has it`() {
        blockCustomer.execute(BlockCustomerCommand(support, customerId, "a reason that is long enough"))
        audit.events.clear()

        val unblocked = unblockCustomer.execute(UnblockCustomerCommand(support, customerId))

        assertFalse(unblocked.isBlocked)
        assertEquals(AuditAction.CUSTOMER_UNBLOCKED, audit.events.single().action)
    }
}
