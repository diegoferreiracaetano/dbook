package com.dbook.application.identity.twofactor

import com.dbook.domain.identity.AccountBlockedException
import com.dbook.domain.identity.InvalidTokenException
import com.dbook.domain.identity.InvalidTwoFactorCodeException
import com.dbook.domain.identity.TooManyLoginAttemptsException
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TheSecondStepOpensTheSessionWithAGoodCodeOnlyOnceTest : TwoFactorFixture() {
    private fun challenge() = challengeOf(staffLogin.execute(staffPassword()))

    @Test
    fun `given a challenge and the code on the phone when verified then the session opens`() {
        enrolled()
        val challenge = challenge()

        val tokens = verify.execute(verifyCommand(challenge, codeOnThePhone()))

        assertEquals("access-1", tokens.accessToken)
        assertEquals(1.0, meters.counter("dbook.auth.login", "outcome", "success", "audience", "staff").count())
    }

    @Test
    fun `given a code that was just used when it is sent again then it is refused as a replay`() {
        enrolled()
        val challenge = challenge()
        val code = codeOnThePhone()
        verify.execute(verifyCommand(challenge, code))

        assertFailsWith<InvalidTwoFactorCodeException> { verify.execute(verifyCommand(challenge, code)) }
    }

    @Test
    fun `given a code from the previous step when it is sent after a newer one was used then it is refused`() {
        enrolled()
        val challenge = challenge()
        val older = codeOnThePhone()
        nextThirtySeconds()
        verify.execute(verifyCommand(challenge, codeOnThePhone()))

        assertFailsWith<InvalidTwoFactorCodeException> { verify.execute(verifyCommand(challenge, older)) }
    }

    @Test
    fun `given a code made one step ahead when verified then it is accepted for the clock that drifts`() {
        enrolled()
        val challenge = challenge()
        nextThirtySeconds()
        val ahead = codeOnThePhone()
        clock.advance(Duration.ofSeconds(-30))

        verify.execute(verifyCommand(challenge, ahead))
    }

    @Test
    fun `given a code from two steps ago when verified then it is refused`() {
        enrolled()
        val challenge = challenge()
        val stale = codeOnThePhone()
        repeat(2) { nextThirtySeconds() }

        assertFailsWith<InvalidTwoFactorCodeException> { verify.execute(verifyCommand(challenge, stale)) }
    }

    @Test
    fun `given wrong codes when they pile up then the account is locked even for the right code`() {
        enrolled()
        val challenge = challenge()
        repeat(
            3,
        ) { assertFailsWith<InvalidTwoFactorCodeException> { verify.execute(verifyCommand(challenge, "000000")) } }

        assertFailsWith<TooManyLoginAttemptsException> { verify.execute(verifyCommand(challenge, codeOnThePhone())) }
    }

    @Test
    fun `given something that is not a challenge when verified then it is an invalid token`() {
        assertFailsWith<InvalidTokenException> { verify.execute(verifyCommand("an-access-token", "123456")) }
    }

    @Test
    fun `given the account is blocked after the password step when the code comes then it is refused`() {
        enrolled()
        val challenge = challenge()
        users.save(root.block("left", clock.instant()))

        assertFailsWith<AccountBlockedException> { verify.execute(verifyCommand(challenge, codeOnThePhone())) }
    }
}
