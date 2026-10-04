package com.dbook.domain.identity.role

import com.dbook.domain.identity.Permission
import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertFalse

class SupportCannotUseTheSensitivePermissionsTest {
    @Test
    fun `given the SUPPORT role when asked about the sensitive permissions then it has none of them`() {
        listOf(Permission.CUSTOMER_EXPORT, Permission.CUSTOMER_ERASE, Permission.ADMIN_MANAGE, Permission.AUDIT_READ)
            .forEach { assertFalse(Role.SUPPORT.can(it), "SUPPORT must not have $it") }
    }
}
