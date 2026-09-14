package com.dbook.application.updateusernameusecase

import com.dbook.application.UpdateUserNameCommand
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdatesTheUsersNameTest : UpdateUserNameUseCaseFixture() {
    @Test
    fun `given a valid new name when updating then it is saved and returned`() {
        val updated = useCase.execute(UpdateUserNameCommand(userId = existingUser.id!!, name = "New Name"))

        assertEquals("New Name", updated.name)
        assertEquals("New Name", userRepository.findById(existingUser.id!!)?.name)
    }
}
