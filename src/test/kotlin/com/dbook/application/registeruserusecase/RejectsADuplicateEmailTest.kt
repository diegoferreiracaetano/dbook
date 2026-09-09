package com.dbook.application.registeruserusecase

import com.dbook.application.RegisterUserCommand
import com.dbook.domain.UserAlreadyExistsException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsADuplicateEmailTest : RegisterUserUseCaseFixture() {
    @Test
    fun `given an already-registered email when registering again then it throws UserAlreadyExistsException`() {
        useCase.execute(RegisterUserCommand(email = "diego@example.com", password = "s3cret"))

        assertFailsWith<UserAlreadyExistsException> {
            useCase.execute(RegisterUserCommand(email = "diego@example.com", password = "other"))
        }
    }
}
