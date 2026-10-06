package com.dbook.application.identity.twofactor

import com.dbook.application.identity.ResetTwoFactorCommand
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.access.Role
import com.dbook.domain.common.audit.AuditAction
import com.dbook.domain.identity.UserNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AnotherSuperAdminCanResetTheSecondFactorOnTheRecordTest : TwoFactorFixture() {
    private val otherActor = Actor(2, Role.SUPER_ADMIN)

    @Test
    fun `given a staff member who lost the authenticator when another admin resets then it is removed and audited`() {
        enrolled(userId = 1)

        reset.execute(ResetTwoFactorCommand(otherActor, 1, "lost the phone and the recovery codes"))

        assertNull(repository.find(1))
        assertEquals(listOf(1L), refreshTokens.revokedForUsers)
        val event = audit.events.single { it.action == AuditAction.TWO_FACTOR_RESET }
        assertEquals("1", event.targetId)
        assertEquals("lost the phone and the recovery codes", event.reason)
        assertEquals(2L, event.actor.id)
    }

    @Test
    fun `given nobody else when resetting their own then it is refused`() {
        enrolled(userId = 1)

        assertFailsWith<IllegalStateException> {
            reset.execute(ResetTwoFactorCommand(rootActor, 1, "I would rather not carry the phone"))
        }

        assertEquals(true, repository.find(1)?.isActive)
    }

    @Test
    fun `given a reason that is too short when resetting then it is a bad request`() {
        enrolled(userId = 1)

        assertFailsWith<IllegalArgumentException> { reset.execute(ResetTwoFactorCommand(otherActor, 1, "lost")) }
    }

    @Test
    fun `given a customer id when resetting then it looks like an account that does not exist`() {
        assertFailsWith<UserNotFoundException> {
            reset.execute(
                ResetTwoFactorCommand(otherActor, 3, "a long enough reason"),
            )
        }
    }

    @Test
    fun `given a staff member without a second factor when resetting then it is a conflict`() {
        assertFailsWith<IllegalStateException> {
            reset.execute(
                ResetTwoFactorCommand(otherActor, 1, "a long enough reason"),
            )
        }
    }
}
