package com.dbook.application.identity.accountrecovery

import com.dbook.domain.identity.InvalidAccountTokenException
import com.dbook.domain.identity.TooManyLoginAttemptsException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ANewConfirmationLinkClosesTheOlderOneTest : AccountRecoveryFixture() {
    @Test
    fun `given an unconfirmed customer when a new link is asked then the old one stops working`() {
        committed { resendVerification.execute(1, "10.0.0.1") }
        val first = tokenInLastMail()
        committed { resendVerification.execute(1, "10.0.0.1") }
        val second = tokenInLastMail()

        assertFailsWith<InvalidAccountTokenException> { verifyEmail.execute(first) }
        verifyEmail.execute(second)
        assertEquals(true, users.findById(1)?.isEmailVerified)
    }

    @Test
    fun `given a confirmed address when a new link is asked then it is a conflict`() {
        committed { resendVerification.execute(1, "10.0.0.1") }
        verifyEmail.execute(tokenInLastMail())

        assertFailsWith<IllegalStateException> { committed { resendVerification.execute(1, "10.0.0.1") } }
    }

    @Test
    fun `given too many requests when one more is made then it is refused and nothing is mailed`() {
        repeat(3) { committed { resendVerification.execute(1, "10.0.0.1") } }
        val sent = mailbox.sent.size

        assertFailsWith<TooManyLoginAttemptsException> { committed { resendVerification.execute(1, "10.0.0.1") } }

        assertEquals(sent, mailbox.sent.size)
    }
}
