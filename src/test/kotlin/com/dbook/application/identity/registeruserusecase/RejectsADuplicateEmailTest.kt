package com.dbook.application.identity.registeruserusecase

import com.dbook.application.identity.RegisterUserCommand
import com.dbook.domain.identity.UserAlreadyExistsException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsADuplicateEmailTest : RegisterUserUseCaseFixture() {
    @Test
    fun `given an already-registered email when registering again then it throws UserAlreadyExistsException`() {
        useCase.execute(RegisterUserCommand(email = "diego@example.com", password = "s3cret-password", name = "Diego"))

        assertFailsWith<UserAlreadyExistsException> {
            useCase.execute(
                RegisterUserCommand(email = "diego@example.com", password = "other-password", name = "Diego"),
            )
        }
    }
}
