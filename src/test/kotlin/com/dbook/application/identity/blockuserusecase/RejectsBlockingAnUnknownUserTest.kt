package com.dbook.application.identity.blockuserusecase

import com.dbook.application.identity.BlockUserCommand
import com.dbook.domain.identity.UserNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsBlockingAnUnknownUserTest : BlockUserUseCaseFixture() {
    @Test
    fun `given an unknown user when blocking then it throws UserNotFoundException`() {
        assertFailsWith<UserNotFoundException> { block.execute(BlockUserCommand(userId = 99, reason = "spam")) }
    }
}
