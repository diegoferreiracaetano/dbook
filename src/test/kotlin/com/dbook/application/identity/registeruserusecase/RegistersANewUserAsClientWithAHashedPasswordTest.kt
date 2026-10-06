package com.dbook.application.identity.registeruserusecase

import com.dbook.application.identity.RegisterUserCommand
import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class RegistersANewUserAsClientWithAHashedPasswordTest : RegisterUserUseCaseFixture() {
    @Test
    fun `given a new email when registering then the user is CLIENT with a hashed password`() {
        val user =
            register(
                RegisterUserCommand(email = "diego@example.com", password = "s3cret-password", name = "Diego"),
            )

        assertEquals(Role.CLIENT, user.role)
        assertEquals("hashed:s3cret-password", user.passwordHash)
        assertEquals("Diego", user.name)
    }
}
