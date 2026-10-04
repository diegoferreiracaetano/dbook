package com.dbook.application.identity.blockuserusecase

import com.dbook.application.identity.BlockUserCommand
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsBlockingAnAlreadyBlockedUserTest : BlockUserUseCaseFixture() {
    @Test
    fun `given a blocked user when blocking again then it throws IllegalStateException`() {
        block.execute(BlockUserCommand(userId = 1, reason = "spam"))

        assertFailsWith<IllegalStateException> { block.execute(BlockUserCommand(userId = 1, reason = "spam again")) }
    }
}
