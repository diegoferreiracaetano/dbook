package com.dbook.application.identity.registeruserusecase

import com.dbook.application.identity.RegisterUserCommand
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RejectsAWeakPasswordTest : RegisterUserUseCaseFixture() {
    @Test
    fun `given a password that breaks the policy when registering then it throws and nothing is saved`() {
        assertFailsWith<IllegalArgumentException> {
            useCase.execute(RegisterUserCommand(email = "diego@example.com", password = "short", name = "Diego"))
        }

        assertTrue(userRepository.users.isEmpty())
    }
}
