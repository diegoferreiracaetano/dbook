package com.dbook.application.identity.twofactor

import com.dbook.application.identity.LoginCommand
import com.dbook.application.identity.SessionAudience
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.TwoFactorRequiredException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// The client endpoint is not a way around the portal's second step: it would sign a staff member in with the password
// alone, and its access token opens every /admin endpoint just the same.
class AStaffAccountWithASecondFactorDoesNotUseTheClientLoginTest : TwoFactorFixture() {
    private fun clientLoginOf(email: String) =
        clientLogin.execute(LoginCommand(email, "correct-password", "10.0.0.1", SessionAudience.CLIENT))

    @Test
    fun `given an active second factor when the staff member uses the client login then it is refused`() {
        enrolled()

        assertFailsWith<TwoFactorRequiredException> { clientLoginOf("root@example.com") }

        assertEquals(0, refreshTokens.saved.size)
        assertEquals(
            1.0,
            meters.counter("dbook.auth.login", "outcome", "second_factor_required", "audience", "client").count(),
        )
    }

    @Test
    fun `given a staff member with none and none required when using the client login then it works as before`() {
        clientLoginOf("root@example.com")

        assertEquals(1, refreshTokens.saved.size)
    }

    @Test
    fun `given a customer when using the client login then the second factor never comes into it`() {
        clientLoginOf("customer@example.com")
    }
}

class ARequiredRoleDoesNotUseTheClientLoginEitherTest : TwoFactorFixture() {
    override val requiredRoles = setOf(Role.SUPER_ADMIN)

    @Test
    fun `given a required role without a second factor when using the client login then it is refused`() {
        assertFailsWith<TwoFactorRequiredException> {
            clientLogin.execute(
                LoginCommand("root@example.com", "correct-password", "10.0.0.1", SessionAudience.CLIENT),
            )
        }
    }
}
