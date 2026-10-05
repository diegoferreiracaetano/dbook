package com.dbook.application.crm.anonymizecustomerusecase

import com.dbook.application.crm.AnonymizeCustomerCommand
import com.dbook.application.crm.CrmUseCaseFixture
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class NeedsTheConfirmationPhraseAndAReasonTest : CrmUseCaseFixture() {
    @Test
    fun `given a wrong phrase, another customer's phrase or a short reason when anonymizing then it is refused`() {
        listOf(
            AnonymizeCustomerCommand(superAdmin, customerId, "a reason that is long enough", "anonymize 3"),
            AnonymizeCustomerCommand(superAdmin, customerId, "a reason that is long enough", "ANONYMIZE 4"),
            AnonymizeCustomerCommand(superAdmin, customerId, "too short", "ANONYMIZE 3"),
        ).forEach { command -> assertFailsWith<IllegalArgumentException> { anonymizeCustomer.execute(command) } }

        assertFalse(users.findById(customerId)?.isAnonymized == true)
        assertFalse(anonymizedEmails.isRemembered("customer@example.com"))
    }
}
