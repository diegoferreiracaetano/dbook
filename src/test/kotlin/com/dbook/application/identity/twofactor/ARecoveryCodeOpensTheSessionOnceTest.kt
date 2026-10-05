package com.dbook.application.identity.twofactor

import com.dbook.domain.identity.InvalidTwoFactorCodeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ARecoveryCodeOpensTheSessionOnceTest : TwoFactorFixture() {
    @Test
    fun `given a recovery code when used instead of the authenticator then the session opens and the code is spent`() {
        val codes = enrolled()
        val challenge = challengeOf(staffLogin.execute(staffPassword()))

        verify.execute(verifyCommand(challenge, codes.first()))

        assertEquals(9, repository.unspentRecoveryCodes(1))
        assertFailsWith<InvalidTwoFactorCodeException> { verify.execute(verifyCommand(challenge, codes.first())) }
    }

    @Test
    fun `given a recovery code typed in lower case with spaces when used then it still counts`() {
        val codes = enrolled()
        val challenge = challengeOf(staffLogin.execute(staffPassword()))

        verify.execute(verifyCommand(challenge, "  ${codes[1].lowercase()} "))
    }

    @Test
    fun `given a code that is not one of theirs when used then it is refused`() {
        enrolled()
        val challenge = challengeOf(staffLogin.execute(staffPassword()))

        assertFailsWith<InvalidTwoFactorCodeException> { verify.execute(verifyCommand(challenge, "ZZZZZ-ZZZZZ")) }
        assertEquals(10, repository.unspentRecoveryCodes(1))
    }
}
