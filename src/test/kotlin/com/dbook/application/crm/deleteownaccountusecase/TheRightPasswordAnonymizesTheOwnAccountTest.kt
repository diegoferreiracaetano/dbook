package com.dbook.application.crm.deleteownaccountusecase

import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.application.crm.DeleteOwnAccountCommand
import com.dbook.domain.common.audit.AuditAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TheRightPasswordAnonymizesTheOwnAccountTest : CrmUseCaseFixture() {
    @Test
    fun `given the right password when a customer deletes their account then it is anonymized by them`() {
        deleteOwnAccount.execute(DeleteOwnAccountCommand(customerId, "current-password-1", "10.0.0.1"))

        assertTrue(users.findById(customerId)?.isAnonymized == true)
        val event = audit.events.single()
        assertEquals(AuditAction.CUSTOMER_ANONYMIZED, event.action)
        assertEquals(customerId, event.actor.id)
        assertEquals("Requested by the customer", event.reason)
    }
}
