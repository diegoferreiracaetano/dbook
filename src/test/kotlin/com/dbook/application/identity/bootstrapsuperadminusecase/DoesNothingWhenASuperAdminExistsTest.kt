package com.dbook.application.identity.bootstrapsuperadminusecase

import com.dbook.application.identity.staff.InMemoryUserRepository
import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DoesNothingWhenASuperAdminExistsTest : BootstrapSuperAdminUseCaseFixture() {
    override val users =
        InMemoryUserRepository(
            User(id = 1, email = "boss@example.com", passwordHash = "x", name = "Boss", role = Role.SUPER_ADMIN),
        )

    @Test
    fun `given a SUPER_ADMIN when bootstrapping then nothing is created`() {
        assertFalse(bootstrap(email = "other@example.com"))

        assertEquals("boss@example.com", superAdmin()?.email)
    }
}
