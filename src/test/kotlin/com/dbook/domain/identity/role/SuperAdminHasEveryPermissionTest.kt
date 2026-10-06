package com.dbook.domain.identity.role

import com.dbook.domain.common.access.Permission
import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class SuperAdminHasEveryPermissionTest {
    @Test
    fun `given the SUPER_ADMIN role when its permissions are listed then it has all of them`() {
        assertEquals(Permission.entries.toSet(), Role.SUPER_ADMIN.permissions)
    }
}
