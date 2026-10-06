package com.dbook.domain.identity.role

import com.dbook.domain.common.access.Permission
import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class EveryStaffRoleCanEnterThePortalTest {
    @Test
    fun `given the roles when asked who is staff then exactly the ones with portal access are`() {
        val withPortalAccess = Role.entries.filter { it.can(Permission.ADMIN_PORTAL_ACCESS) }

        assertEquals(listOf(Role.SUPPORT, Role.CATALOG_MANAGER, Role.SUPER_ADMIN), withPortalAccess)
        assertEquals(withPortalAccess, Role.entries.filter { it.isStaff })
    }
}
