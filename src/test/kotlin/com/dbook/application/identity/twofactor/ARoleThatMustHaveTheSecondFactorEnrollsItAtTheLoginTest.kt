package com.dbook.application.identity.twofactor

import com.dbook.application.identity.StaffLoginOutcome
import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.InvalidTokenException
import com.dbook.domain.identity.InvalidTwoFactorCodeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ARoleThatMustHaveTheSecondFactorEnrollsItAtTheLoginTest : TwoFactorFixture() {
    override val requiredRoles = setOf(Role.SUPER_ADMIN)

    @Test
    fun `given a required role without a second factor when signing in then the answer is to enroll`() {
        val outcome = staffLogin.execute(staffPassword())

        assertEquals(StaffLoginOutcome.Challenge("challenge-ENROLL-1", enrollmentRequired = true), outcome)
        assertEquals(0, refreshTokens.saved.size)
    }

    @Test
    fun `given the enrollment challenge when the first code is confirmed then the session opens`() {
        val challenge = challengeOf(staffLogin.execute(staffPassword()))
        val start = startEnrollmentAtLogin.execute(challenge)
        assertTrue(
            start.otpauthUri.startsWith("otpauth://totp/DBook:root%40example.com?secret=${start.manualEntryKey}"),
        )

        val enrolled = confirmEnrollmentAtLogin.execute(verifyCommand(challenge, codeOnThePhone()))

        assertEquals(10, enrolled.recoveryCodes.size)
        assertEquals(1, refreshTokens.saved.size)
        assertEquals(10, repository.unspentRecoveryCodes(1))
    }

    @Test
    fun `given the enrollment challenge when the code is wrong then there is no session`() {
        val challenge = challengeOf(staffLogin.execute(staffPassword()))
        startEnrollmentAtLogin.execute(challenge)

        assertFailsWith<InvalidTwoFactorCodeException> {
            confirmEnrollmentAtLogin.execute(verifyCommand(challenge, "000000"))
        }

        assertEquals(0, refreshTokens.saved.size)
        assertEquals(false, repository.find(1)?.isActive)
    }

    @Test
    fun `given a verify challenge when it is used to enroll then it is refused`() {
        assertFailsWith<InvalidTokenException> { startEnrollmentAtLogin.execute("challenge-VERIFY-1") }
    }
}
