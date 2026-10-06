package com.dbook.presentation.customernotes

import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class TheBlockIsAuditedWithItsReasonTest : CustomerNotesFixture() {
    @Test
    fun `given a blocked customer when reading the audit then the entry has the reason and both statuses`() {
        val id = newCustomerId()
        blockCustomer(staffToken(), id, "Chargeback fraud confirmed")

        val entry = auditEntries(staffToken(Role.SUPER_ADMIN), "action=CUSTOMER_BLOCKED&targetId=$id")[0]

        assertEquals("Chargeback fraud confirmed", entry["reason"].asText())
        assertEquals("ACTIVE", entry["before"]["status"].asText())
        assertEquals("BLOCKED", entry["after"]["status"].asText())
    }
}
