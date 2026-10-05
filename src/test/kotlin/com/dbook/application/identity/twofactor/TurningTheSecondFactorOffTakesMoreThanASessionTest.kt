package com.dbook.application.identity.twofactor

import com.dbook.application.identity.DisableTwoFactorCommand
import com.dbook.domain.audit.AuditAction
import com.dbook.domain.identity.InvalidCredentialsException
import com.dbook.domain.identity.InvalidTwoFactorCodeException
import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TurningTheSecondFactorOffTakesMoreThanASessionTest : TwoFactorFixture() {
    private fun disableCommand(
        password: String = "correct-password",
        code: String = codeOnThePhone(),
    ) = DisableTwoFactorCommand(rootActor, password, code, "10.0.0.1")

    @Test
    fun `given the password and a code when disabling then the second factor is gone and it is audited`() {
        enrolled()

        disable.execute(disableCommand())

        assertNull(repository.find(1))
        assertEquals(AuditAction.TWO_FACTOR_DISABLED, audit.events.last().action)
    }

    @Test
    fun `given a wrong password when disabling then it is refused and the second factor stays`() {
        enrolled()

        assertFailsWith<InvalidCredentialsException> { disable.execute(disableCommand(password = "wrong")) }

        assertEquals(true, repository.find(1)?.isActive)
    }

    @Test
    fun `given a wrong code when disabling then it is refused and the second factor stays`() {
        enrolled()

        assertFailsWith<InvalidTwoFactorCodeException> { disable.execute(disableCommand(code = "000000")) }

        assertEquals(true, repository.find(1)?.isActive)
    }

    @Test
    fun `given no second factor when disabling then it is a conflict`() {
        assertFailsWith<IllegalStateException> { disable.execute(disableCommand(code = "123456")) }
    }
}

class ARoleThatMustHaveItCannotTurnItOffTest : TwoFactorFixture() {
    override val requiredRoles = setOf(Role.SUPER_ADMIN)

    @Test
    fun `given a required role when disabling then it is refused, with a valid password and code`() {
        enrolled()

        assertFailsWith<IllegalStateException> {
            disable.execute(DisableTwoFactorCommand(rootActor, "correct-password", codeOnThePhone(), "10.0.0.1"))
        }

        assertEquals(true, repository.find(1)?.isActive)
    }
}
