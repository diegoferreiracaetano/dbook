package com.dbook.presentation.customerprivacy

import kotlin.test.Test
import kotlin.test.assertEquals

class AnExportIsRecordedWithItsFilterTest : CustomerPrivacyFixture() {
    @Test
    fun `given an export when reading the audit then the entry has the filter that was used`() {
        val tag = newTag()
        val admin = superAdminToken()
        exportCsv(admin, "query" to tag, "status" to "BLOCKED")

        val entry = auditEntries(admin, "action=CUSTOMER_EXPORTED").first { it["after"]["text"].asText() == tag }

        assertEquals("BLOCKED", entry["after"]["status"].asText())
        assertEquals(50_000, entry["after"]["maxRows"].asInt())
    }
}
