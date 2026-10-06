package com.dbook.domain.common.access

import com.dbook.domain.common.access.Permission.ADMIN_PORTAL_ACCESS
import com.dbook.domain.common.access.Permission.BOOKING_CANCEL_ANY
import com.dbook.domain.common.access.Permission.BOOKING_READ_ANY
import com.dbook.domain.common.access.Permission.CATALOG_WRITE
import com.dbook.domain.common.access.Permission.CUSTOMER_BLOCK
import com.dbook.domain.common.access.Permission.CUSTOMER_NOTE
import com.dbook.domain.common.access.Permission.CUSTOMER_READ
import com.dbook.domain.common.access.Permission.DASHBOARD_READ
import com.dbook.domain.common.access.Permission.FLIGHT_READ
import com.dbook.domain.common.access.Permission.FLIGHT_WRITE
import com.dbook.domain.common.access.Permission.PAYMENT_REFUND
import com.dbook.domain.common.access.Permission.PROMO_WRITE
import com.dbook.domain.common.access.Permission.REVIEW_MODERATE

// The permissions of each role live here and are resolved on every request; the token only carries the role name.
enum class Role(val permissions: Set<Permission>) {
    CLIENT(emptySet()),
    SUPPORT(
        setOf(
            ADMIN_PORTAL_ACCESS,
            CUSTOMER_READ,
            CUSTOMER_NOTE,
            CUSTOMER_BLOCK,
            BOOKING_READ_ANY,
            BOOKING_CANCEL_ANY,
            PAYMENT_REFUND,
            REVIEW_MODERATE,
            DASHBOARD_READ,
        ),
    ),
    CATALOG_MANAGER(
        setOf(
            ADMIN_PORTAL_ACCESS,
            FLIGHT_READ,
            FLIGHT_WRITE,
            CATALOG_WRITE,
            PROMO_WRITE,
            DASHBOARD_READ,
        ),
    ),
    SUPER_ADMIN(Permission.entries.toSet()),
    ;

    val isStaff: Boolean get() = ADMIN_PORTAL_ACCESS in permissions

    fun can(permission: Permission): Boolean = permission in permissions
}
