package com.dbook.application.updateusernameusecase

import com.dbook.application.UpdateUserNameCommand
import com.dbook.domain.UserNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenUserDoesNotExistTest : UpdateUserNameUseCaseFixture() {
    @Test
    fun `given an unknown user id when updating then it throws UserNotFoundException`() {
        assertFailsWith<UserNotFoundException> {
            useCase.execute(UpdateUserNameCommand(userId = 999, name = "New Name"))
        }
    }
}
