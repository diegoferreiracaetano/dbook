package com.dbook.application.identity.accountrecovery

import com.dbook.application.identity.ResetPasswordCommand
import com.dbook.domain.identity.AccountTokenPurpose
import com.dbook.domain.identity.InvalidAccountTokenException
import com.dbook.domain.identity.TooManyLoginAttemptsException
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AForgottenPasswordIsAskedForWithoutRevealingWhoHasAnAccountTest : AccountRecoveryFixture() {
    @Test
    fun `given a customer when the reset is asked then the link goes to the app and valid for one hour`() {
        committed { requestReset.execute("ana@example.com", "10.0.0.1") }

        val mail = mailbox.sent.single()
        assertEquals("ana@example.com", mail.to)
        assertTrue(mail.body.contains("https://app.test/reset-password?token=token-1"))
        val stored = tokens.all.single()
        assertEquals(AccountTokenPurpose.PASSWORD_RESET, stored.purpose)
        assertEquals(clock.instant().plus(Duration.ofHours(1)), stored.expiresAt)
    }

    @Test
    fun `given a staff member when the reset is asked then the link goes to the portal`() {
        committed { requestReset.execute("boss@example.com", "10.0.0.1") }

        assertTrue(mailbox.sent.single().body.contains("https://portal.test/reset-password?token=token-1"))
    }

    @Test
    fun `given the address, with other letter case and spaces, when asked then it is the same account`() {
        committed { requestReset.execute("  Ana@Example.COM ", "10.0.0.1") }

        assertEquals("ana@example.com", mailbox.sent.single().to)
    }

    @Test
    fun `given an address with no account when the reset is asked then it answers the same and sends nothing`() {
        committed { requestReset.execute("nobody@example.com", "10.0.0.1") }

        assertEquals(0, mailbox.sent.size)
        assertEquals(0, tokens.all.size)
        assertEquals(1.0, meters.counter("dbook.account.password_reset", "outcome", "no_account").count())
    }

    @Test
    fun `given a blocked account when the reset is asked then it answers the same and sends nothing`() {
        committed { requestReset.execute("bob@example.com", "10.0.0.1") }

        assertEquals(0, mailbox.sent.size)
    }

    @Test
    fun `given the same address asked too often when one more comes then it is refused, account or not`() {
        repeat(3) { committed { requestReset.execute("nobody@example.com", "10.0.0.${it + 1}") } }

        assertFailsWith<TooManyLoginAttemptsException> {
            committed { requestReset.execute("nobody@example.com", "10.0.0.9") }
        }
    }

    @Test
    fun `given a new request when it is made then the older link stops working`() {
        committed { requestReset.execute("ana@example.com", "10.0.0.1") }
        val first = tokenInLastMail()
        committed { requestReset.execute("ana@example.com", "10.0.0.1") }

        assertFailsWith<InvalidAccountTokenException> {
            resetPassword.execute(ResetPasswordCommand(first, "a-brand-new-password"))
        }
    }
}
