package com.dbook.presentation.catalogadmin

import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class EditingAndCancellingAreAuditedWithBeforeAndAfterTest : CatalogAdminFixture() {
    @Test
    fun `given an edit and a cancellation when reading the audit then both have the states before and after`() {
        val token = manager()
        val admin = registerStaffAndLogin(uniqueEmail(), Role.SUPER_ADMIN)
        val id = createFlight(token)
        edit(token, id, mapOf("price" to 150.0))
        cancelFlight(token, id)

        val trail = auditEntries(admin, "targetType=FLIGHT&targetId=$id")

        val updated = trail.first { it["action"].asText() == "FLIGHT_UPDATED" }
        assertEquals(100.0, updated["before"]["price"].asDouble())
        assertEquals(150.0, updated["after"]["price"].asDouble())
        val cancelled = trail.first { it["action"].asText() == "FLIGHT_CANCELLED" }
        assertEquals("SCHEDULED", cancelled["before"]["status"].asText())
        assertEquals("CANCELLED", cancelled["after"]["status"].asText())
    }
}
