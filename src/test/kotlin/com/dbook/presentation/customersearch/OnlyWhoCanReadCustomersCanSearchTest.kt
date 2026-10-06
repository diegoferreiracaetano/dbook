package com.dbook.presentation.customersearch

import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyWhoCanReadCustomersCanSearchTest : CustomerSearchFixture() {
    @Test
    fun `given a catalog manager when searching then it is 403, and a support member gets 200`() {
        val catalogToken = registerStaffAndLogin(uniqueEmail(), Role.CATALOG_MANAGER)

        assertEquals(403, search(catalogToken).response.status)
        assertEquals(200, search(supportToken()).response.status)
    }
}
