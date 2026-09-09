package com.dbook.application.registeruserusecase

import com.dbook.application.RegisterUserCommand
import com.dbook.domain.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class RegistersANewUserAsClientWithAHashedPasswordTest : RegisterUserUseCaseFixture() {
    @Test
    fun `given a new email when registering then the user is CLIENT with a hashed password`() {
        val user = useCase.execute(RegisterUserCommand(email = "diego@example.com", password = "s3cret"))

        assertEquals(Role.CLIENT, user.role)
        assertEquals("hashed:s3cret", user.passwordHash)
    }
}
