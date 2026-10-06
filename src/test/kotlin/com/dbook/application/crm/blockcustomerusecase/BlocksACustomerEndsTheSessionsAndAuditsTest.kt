package com.dbook.application.crm.blockcustomerusecase

import com.dbook.application.crm.BlockCustomerCommand
import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.domain.common.audit.AuditAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlocksACustomerEndsTheSessionsAndAuditsTest : CrmUseCaseFixture() {
    @Test
    fun `given a customer when blocking then they are blocked, sessions end and the audit has the reason`() {
        val blocked = blockCustomer.execute(BlockCustomerCommand(support, customerId, "  Chargeback fraud confirmed "))

        assertTrue(blocked.isBlocked)
        assertEquals(listOf(customerId), refreshTokens.revokedForUsers)
        val event = audit.events.single()
        assertEquals(AuditAction.CUSTOMER_BLOCKED, event.action)
        assertEquals("Chargeback fraud confirmed", event.reason)
        assertEquals("BLOCKED", event.after?.get("status"))
    }
}
