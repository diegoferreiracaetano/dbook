package com.dbook.application.crm.anonymizecustomerusecase

import com.dbook.application.crm.AnonymizeCustomerCommand
import com.dbook.application.crm.CrmUseCaseFixture
import com.dbook.domain.audit.AuditAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnonymizesTheAccountAndEverythingAroundItTest : CrmUseCaseFixture() {
    @Test
    fun `given a customer when anonymizing then the account, the sessions, the notes and the address are dealt with`() {
        val result =
            anonymizeCustomer.execute(
                AnonymizeCustomerCommand(superAdmin, customerId, "Erasure requested by e-mail", "ANONYMIZE 3"),
            )

        assertTrue(result.isAnonymized)
        assertEquals("anonymized-3@anonymous.invalid", users.findById(customerId)?.email)
        assertEquals(listOf(customerId), refreshTokens.revokedForUsers)
        assertEquals(listOf(customerId), erasure.erased)
        assertTrue(anonymizedEmails.isRemembered("customer@example.com"))
        val event = audit.events.single()
        assertEquals(AuditAction.CUSTOMER_ANONYMIZED, event.action)
        assertEquals("Erasure requested by e-mail", event.reason)
        assertFalse(event.before.toString().contains("customer@example.com"))
        assertFalse(event.after.toString().contains("Customer"))
    }
}
