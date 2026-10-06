package com.dbook.domain.identity.role

import com.dbook.domain.common.access.Permission
import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogManagerCannotTouchCustomersNorMoneyTest {
    @Test
    fun `given the CATALOG_MANAGER role when asked about customers and refunds then it has neither`() {
        assertTrue(Role.CATALOG_MANAGER.can(Permission.FLIGHT_WRITE))
        assertTrue(Role.CATALOG_MANAGER.can(Permission.PROMO_WRITE))
        assertFalse(Role.CATALOG_MANAGER.can(Permission.CUSTOMER_READ))
        assertFalse(Role.CATALOG_MANAGER.can(Permission.PAYMENT_REFUND))
        assertFalse(Role.CATALOG_MANAGER.can(Permission.BOOKING_CANCEL_ANY))
    }
}
