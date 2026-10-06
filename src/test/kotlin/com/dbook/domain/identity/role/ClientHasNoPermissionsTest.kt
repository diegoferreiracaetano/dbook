package com.dbook.domain.identity.role

import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientHasNoPermissionsTest {
    @Test
    fun `given the CLIENT role when its permissions are listed then there are none and it is not staff`() {
        assertTrue(Role.CLIENT.permissions.isEmpty())
        assertFalse(Role.CLIENT.isStaff)
    }
}
