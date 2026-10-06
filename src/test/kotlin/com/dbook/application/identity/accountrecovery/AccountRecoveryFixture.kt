package com.dbook.application.identity.accountrecovery

import com.dbook.MutableClock
import com.dbook.application.identity.AccountLinkIssuer
import com.dbook.application.identity.AccountMailer
import com.dbook.application.identity.AdminPortalLinks
import com.dbook.application.identity.CustomerAppLinks
import com.dbook.application.identity.LoginAttemptGuard
import com.dbook.application.identity.LoginAttemptsPolicy
import com.dbook.application.identity.RecoveryRequestGuard
import com.dbook.application.identity.RegisterUserCommand
import com.dbook.application.identity.RegisterUserUseCase
import com.dbook.application.identity.RequestPasswordResetUseCase
import com.dbook.application.identity.ResendEmailVerificationUseCase
import com.dbook.application.identity.ResetPasswordUseCase
import com.dbook.application.identity.VerifyEmailUseCase
import com.dbook.application.identity.loginusecase.FakeLoginAttemptLimiter
import com.dbook.application.identity.loginusecase.FakeRefreshTokenRepository
import com.dbook.application.identity.loginusecase.FakeTokenService
import com.dbook.application.identity.loginusecase.FixedPasswordHasher
import com.dbook.application.identity.staff.InMemoryAnonymizedEmails
import com.dbook.application.identity.staff.InMemoryUserRepository
import com.dbook.application.identity.staff.RecordingEmailSender
import com.dbook.application.identity.staff.SequentialInvitationTokens
import com.dbook.domain.identity.AccountToken
import com.dbook.domain.identity.AccountTokenRepository
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant

// The same rules as the database's conditional UPDATEs: a link is spent once, never after it expired, and a new one
// of the same purpose closes the older ones.
class InMemoryAccountTokens(private val now: () -> Instant) : AccountTokenRepository {
    val all = mutableListOf<AccountToken>()

    override fun issue(token: AccountToken): AccountToken {
        all.replaceAll {
            if (it.userId == token.userId && it.purpose == token.purpose && it.usedAt == null) {
                it.copy(
                    usedAt = now(),
                )
            } else {
                it
            }
        }
        val stored = token.copy(id = all.size + 1L)
        all += stored
        return stored
    }

    override fun findByTokenHash(tokenHash: String): AccountToken? = all.find { it.tokenHash == tokenHash }

    override fun consume(
        id: Long,
        now: Instant,
    ): Boolean {
        val index = all.indexOfFirst { it.id == id }
        if (!all[index].isUsableAt(now)) return false
        all[index] = all[index].copy(usedAt = now)
        return true
    }
}

// Shared "given": a customer (id 1, password "old-password-1"), a staff member (id 2), a blocked customer (id 3),
// a clock that moves by hand, tokens "token-1", "token-2"... (their hash is "hash:<token>") and a mailbox.
abstract class AccountRecoveryFixture {
    protected val clock = MutableClock(Instant.parse("2026-10-05T12:00:00Z"))
    protected val ana = User(id = 1, email = "ana@example.com", passwordHash = "hashed:old-password-1", name = "Ana")
    protected val boss =
        User(
            id = 2,
            email = "boss@example.com",
            passwordHash = "hashed:old-password-12",
            name = "Boss",
            role = Role.SUPER_ADMIN,
        )
    protected val blocked =
        User(
            id = 3,
            email = "bob@example.com",
            passwordHash = "x",
            name = "Bob",
        ).block("chargeback fraud", Instant.parse("2026-09-01T00:00:00Z"))

    protected val users = InMemoryUserRepository(ana, boss, blocked)
    protected val tokens = InMemoryAccountTokens { clock.instant() }
    protected val mailbox = RecordingEmailSender()
    protected val refreshTokens = FakeRefreshTokenRepository()
    protected val limiter = FakeLoginAttemptLimiter()
    protected val meters = SimpleMeterRegistry()

    private val hasher = FixedPasswordHasher(validPassword = "irrelevant")
    private val tokenService = FakeTokenService()
    private val attemptGuard =
        LoginAttemptGuard(limiter, LoginAttemptsPolicy(maxFailuresPerEmail = 3, maxFailuresPerIp = 5))
    private val recoveryGuard = RecoveryRequestGuard(attemptGuard)
    private val issuer = AccountLinkIssuer(tokens, SequentialInvitationTokens(), tokenService, clock)
    private val mailer =
        AccountMailer(mailbox, CustomerAppLinks("https://app.test"), AdminPortalLinks("https://portal.test"))

    protected val register by lazy {
        RegisterUserUseCase(users, hasher, InMemoryAnonymizedEmails(), issuer, mailer)
    }
    protected val verifyEmail by lazy { VerifyEmailUseCase(tokens, users, tokenService, meters, clock) }
    protected val resendVerification by lazy { ResendEmailVerificationUseCase(users, issuer, mailer, recoveryGuard) }
    protected val requestReset by lazy { RequestPasswordResetUseCase(users, issuer, mailer, recoveryGuard, meters) }
    protected val resetPassword by lazy {
        ResetPasswordUseCase(tokens, users, tokenService, hasher, refreshTokens, attemptGuard, meters, clock)
    }

    /** Runs [action] and then the after-commit callbacks, the way a successful commit would. */
    protected fun <T> committed(action: () -> T): T {
        TransactionSynchronizationManager.initSynchronization()
        try {
            val result = action()
            TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
            return result
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }
    }

    protected fun registerAs(
        email: String,
        password: String = "a-long-enough-password",
    ) = committed { register.execute(RegisterUserCommand(email, password, "New Customer")) }

    /** The token in the link of the last mail: what the customer would click. */
    protected fun tokenInLastMail(): String =
        mailbox.sent.last().body.substringAfter("token=").takeWhile {
            !it.isWhitespace()
        }
}
