package com.dbook.application.identity.accountrecovery

import com.dbook.application.identity.RegisterUserCommand
import com.dbook.domain.identity.AccountTokenPurpose
import com.dbook.domain.identity.InvalidAccountTokenException
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ANewAccountStartsUnconfirmedAndGetsALinkTest : AccountRecoveryFixture() {
    @Test
    fun `given a registration when it commits then the account is unconfirmed and the link is mailed to the address`() {
        val saved = registerAs("new@example.com")

        assertFalse(saved.isEmailVerified)
        val mail = mailbox.sent.single()
        assertEquals("new@example.com", mail.to)
        assertTrue(mail.body.contains("https://app.test/verify-email?token=token-1"))
        val stored = tokens.all.single()
        assertEquals("hash:token-1", stored.tokenHash, "only the hash is kept, never the token that was mailed")
        assertEquals(AccountTokenPurpose.EMAIL_VERIFICATION, stored.purpose)
        assertEquals(clock.instant().plus(Duration.ofHours(48)), stored.expiresAt)
    }

    @Test
    fun `given the mailed link when it is used then the address is confirmed`() {
        val saved = registerAs("new@example.com")

        committed { verifyEmail.execute(tokenInLastMail()) }

        assertTrue(requireNotNull(users.findById(requireNotNull(saved.id))).isEmailVerified)
        assertEquals(1.0, meters.counter("dbook.account.email_verification", "outcome", "verified").count())
    }

    @Test
    fun `given a link already used when it is used again then it is refused`() {
        registerAs("new@example.com")
        val token = tokenInLastMail()
        verifyEmail.execute(token)

        assertFailsWith<InvalidAccountTokenException> { verifyEmail.execute(token) }
    }

    @Test
    fun `given a link older than 48 hours when it is used then it is refused`() {
        registerAs("new@example.com")
        val token = tokenInLastMail()
        clock.advance(Duration.ofHours(49))

        assertFailsWith<InvalidAccountTokenException> { verifyEmail.execute(token) }
    }

    @Test
    fun `given a made up token when it is used then it is refused like any other`() {
        assertFailsWith<InvalidAccountTokenException> { verifyEmail.execute("not-a-token") }
        assertEquals(1.0, meters.counter("dbook.account.email_verification", "outcome", "invalid_link").count())
    }

    @Test
    fun `given a registration that rolls back when it ends then no mail leaves`() {
        TransactionSynchronizationManager.initSynchronization()
        try {
            register.execute(RegisterUserCommand("x@example.com", "a-long-enough-password", "X"))
        } finally {
            TransactionSynchronizationManager.clearSynchronization() // no commit: the callbacks never run
        }

        assertEquals(0, mailbox.sent.size)
    }
}
