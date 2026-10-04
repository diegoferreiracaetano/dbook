package com.dbook.application.identity.bootstrapsuperadminusecase

import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreatesTheFirstSuperAdminTest : BootstrapSuperAdminUseCaseFixture() {
    @Test
    fun `given no SUPER_ADMIN when bootstrapping then one is created with the normalized email`() {
        assertTrue(bootstrap())

        assertEquals("root@example.com", superAdmin()?.email)
        assertEquals(Role.SUPER_ADMIN, superAdmin()?.role)
    }
}
