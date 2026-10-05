package com.dbook.presentation.customerprivacy

import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyASuperAdminCanExportTest : CustomerPrivacyFixture() {
    @Test
    fun `given a support member or a catalog manager when exporting then 403, and a SUPER_ADMIN gets 200`() {
        assertEquals(403, exportCsv(staffToken(Role.SUPPORT)).response.status)
        assertEquals(403, exportCsv(staffToken(Role.CATALOG_MANAGER)).response.status)
        assertEquals(200, exportCsv(superAdminToken(), "query" to newTag()).response.status)
    }
}
