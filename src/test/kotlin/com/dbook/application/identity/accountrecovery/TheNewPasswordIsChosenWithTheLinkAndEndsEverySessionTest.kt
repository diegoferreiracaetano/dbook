package com.dbook.application.identity.accountrecovery

import com.dbook.application.identity.ResetPasswordCommand
import com.dbook.domain.identity.InvalidAccountTokenException
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TheNewPasswordIsChosenWithTheLinkAndEndsEverySessionTest : AccountRecoveryFixture() {
    private fun askedFor(email: String): String {
        committed { requestReset.execute(email, "10.0.0.1") }
        return tokenInLastMail()
    }

    @Test
    fun `given the link when the new password is chosen then it is stored hashed and every session ends`() {
        val token = askedFor("ana@example.com")
        limiter.recordFailure("email:ana@example.com")

        resetPassword.execute(ResetPasswordCommand(token, "a-brand-new-password"))

        assertEquals("hashed:a-brand-new-password", users.findById(1)?.passwordHash)
        assertEquals(listOf(1L), refreshTokens.revokedForUsers)
        assertEquals(0, limiter.failuresOf("email:ana@example.com"), "a locked-out customer can try again")
        assertEquals(1.0, meters.counter("dbook.account.password_reset", "outcome", "completed").count())
    }

    @Test
    fun `given the link when it is used then the address counts as confirmed, the mailbox was proven`() {
        val token = askedFor("ana@example.com")

        resetPassword.execute(ResetPasswordCommand(token, "a-brand-new-password"))

        assertTrue(users.findById(1)?.isEmailVerified == true)
    }

    @Test
    fun `given a link already used when it is used again then it is refused`() {
        val token = askedFor("ana@example.com")
        resetPassword.execute(ResetPasswordCommand(token, "a-brand-new-password"))

        assertFailsWith<InvalidAccountTokenException> {
            resetPassword.execute(ResetPasswordCommand(token, "another-new-password"))
        }
    }

    @Test
    fun `given a link older than one hour when it is used then it is refused`() {
        val token = askedFor("ana@example.com")
        clock.advance(Duration.ofMinutes(61))

        assertFailsWith<InvalidAccountTokenException> {
            resetPassword.execute(ResetPasswordCommand(token, "a-brand-new-password"))
        }
    }

    @Test
    fun `given a weak password when it is sent then it is refused and the link is not burned`() {
        val token = askedFor("ana@example.com")

        assertFailsWith<IllegalArgumentException> { resetPassword.execute(ResetPasswordCommand(token, "short")) }

        resetPassword.execute(ResetPasswordCommand(token, "a-brand-new-password"))
        assertEquals("hashed:a-brand-new-password", users.findById(1)?.passwordHash)
    }

    @Test
    fun `given a staff member when a 10 character password is sent then it is refused, staff need 12`() {
        val token = askedFor("boss@example.com")

        assertFailsWith<IllegalArgumentException> { resetPassword.execute(ResetPasswordCommand(token, "tenchars-ok")) }
    }

    @Test
    fun `given a confirmation link when used to reset a password then it is refused`() {
        registerAs("fresh@example.com")
        val confirmation = tokenInLastMail()

        assertFailsWith<InvalidAccountTokenException> {
            resetPassword.execute(ResetPasswordCommand(confirmation, "a-brand-new-password"))
        }
    }

    @Test
    fun `given an account blocked after the link was sent when it is used then it is refused`() {
        val token = askedFor("ana@example.com")
        users.save(users.findById(1)!!.block("chargeback fraud", clock.instant()))

        assertFailsWith<InvalidAccountTokenException> {
            resetPassword.execute(ResetPasswordCommand(token, "a-brand-new-password"))
        }
    }
}
