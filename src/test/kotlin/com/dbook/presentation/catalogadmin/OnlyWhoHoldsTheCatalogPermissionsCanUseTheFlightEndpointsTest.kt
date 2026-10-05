package com.dbook.presentation.catalogadmin

import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyWhoHoldsTheCatalogPermissionsCanUseTheFlightEndpointsTest : CatalogAdminFixture() {
    @Test
    fun `given support, a customer and a manager when listing and editing then 403, 403 and 200`() {
        val manager = manager()
        val id = createFlight(manager)
        val support = registerStaffAndLogin(uniqueEmail(), Role.SUPPORT)
        val customer = registerAndLogin(uniqueEmail())

        assertEquals(403, searchFlights(support).response.status)
        assertEquals(403, flight(customer, id).response.status)
        assertEquals(403, editBlind(support, id).response.status)
        assertEquals(403, cancelFlight(support, id).response.status)
        assertEquals(200, flight(manager, id).response.status)
    }
}
