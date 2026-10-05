package com.dbook.presentation.customerprivacy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TheAnonymizationTrailNeverHoldsTheCustomersDataTest : CustomerPrivacyFixture() {
    @Test
    fun `given an anonymization when reading the audit then it has the reason and neither the name nor the email`() {
        val email = uniqueEmail()
        registerAndLogin(email, name = "Maria Silva")
        val id = userIdOf(email)
        val admin = superAdminToken()
        anonymize(admin, id, reason = "Erasure requested by the customer")

        val entry = auditEntries(admin, "action=CUSTOMER_ANONYMIZED&targetId=$id")[0]

        assertEquals("Erasure requested by the customer", entry["reason"].asText())
        assertFalse(entry.toString().contains("Maria"))
        assertFalse(entry.toString().contains(email))
    }
}
