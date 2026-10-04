package com.dbook.application.identity.blockuserusecase

import com.dbook.application.identity.BlockUserCommand
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull

class UnblocksABlockedUserTest : BlockUserUseCaseFixture() {
    @Test
    fun `given a blocked user when unblocking then it is active again with no block details`() {
        block.execute(BlockUserCommand(userId = 1, reason = "spam"))

        val unblocked = unblock.execute(1)

        assertFalse(unblocked.isBlocked)
        assertNull(unblocked.blockedReason)
    }
}
