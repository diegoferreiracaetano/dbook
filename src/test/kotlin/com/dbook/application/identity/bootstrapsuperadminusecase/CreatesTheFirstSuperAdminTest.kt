package com.dbook.application.identity.bootstrapsuperadminusecase

import com.dbook.domain.common.access.Role
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

    @Test
    fun `given the first SUPER_ADMIN when created then its address needs no confirmation by mail`() {
        bootstrap()

        assertTrue(superAdmin()?.isEmailVerified == true)
    }
}
