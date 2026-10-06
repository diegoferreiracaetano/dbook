package com.dbook.domain.identity.role

import com.dbook.domain.common.access.Permission
import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SupportCannotWriteTheCatalogTest {
    @Test
    fun `given the SUPPORT role when asked about the catalog then it can read customers but not write flights`() {
        assertTrue(Role.SUPPORT.can(Permission.CUSTOMER_READ))
        assertTrue(Role.SUPPORT.can(Permission.BOOKING_CANCEL_ANY))
        assertFalse(Role.SUPPORT.can(Permission.FLIGHT_WRITE))
        assertFalse(Role.SUPPORT.can(Permission.CATALOG_WRITE))
    }
}
