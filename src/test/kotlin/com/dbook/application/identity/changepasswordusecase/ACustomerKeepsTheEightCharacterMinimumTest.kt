package com.dbook.application.identity.changepasswordusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class ACustomerKeepsTheEightCharacterMinimumTest : ChangePasswordUseCaseFixture() {
    @Test
    fun `given a customer when the new password has eight characters then it is accepted`() {
        change(userId = 2, new = "eight-ok")

        assertEquals("hashed:eight-ok", users.findById(2)?.passwordHash)
    }
}
