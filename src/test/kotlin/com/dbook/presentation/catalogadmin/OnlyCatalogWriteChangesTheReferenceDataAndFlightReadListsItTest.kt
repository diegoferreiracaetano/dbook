package com.dbook.presentation.catalogadmin

import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyCatalogWriteChangesTheReferenceDataAndFlightReadListsItTest : CatalogAdminFixture() {
    @Test
    fun `given support or a customer when changing or listing reference data then 403, and a manager can both`() {
        val manager = manager()
        val support = registerStaffAndLogin(uniqueEmail(), Role.SUPPORT)
        val body = mapOf("iataCode" to "ZZ", "name" to "No")

        assertEquals(403, post(support, "/v1/admin/airlines", body).response.status)
        assertEquals(403, get(support, "/v1/admin/airlines").response.status)
        assertEquals(403, get(registerAndLogin(uniqueEmail()), "/v1/admin/airports").response.status)
        assertEquals(200, get(manager, "/v1/admin/airports").response.status)
    }
}
