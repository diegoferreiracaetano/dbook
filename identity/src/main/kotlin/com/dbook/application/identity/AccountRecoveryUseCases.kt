package com.dbook.application.identity

import com.dbook.application.common.afterCommit
import com.dbook.application.common.countOutcome
import com.dbook.domain.identity.AccountToken
import com.dbook.domain.identity.AccountTokenPurpose
import com.dbook.domain.identity.AccountTokenRepository
import com.dbook.domain.identity.InvalidAccountTokenException
import com.dbook.domain.identity.InvitationTokenGenerator
import com.dbook.domain.identity.PasswordHasher
import com.dbook.domain.identity.PasswordPolicy
import com.dbook.domain.identity.RefreshTokenRepository
import com.dbook.domain.identity.TokenService
import com.dbook.domain.identity.TooManyLoginAttemptsException
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserNotFoundException
import com.dbook.domain.identity.UserRepository
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

/**
 * Makes a link for a user and mails it once the transaction commits (a rollback sends nothing). The token is the same
 * kind of random text as an invitation's; only its hash is stored. Issuing closes the older links of that purpose.
 */
@Service
class AccountLinkIssuer(
    private val accountTokens: AccountTokenRepository,
    private val tokenGenerator: InvitationTokenGenerator,
    private val tokenService: TokenService,
    private val clock: Clock,
) {
    /** The token to mail. Must run inside a transaction: the caller sends the mail after it commits. */
    fun issue(
        user: User,
        purpose: AccountTokenPurpose,
    ): String {
        val token = tokenGenerator.generate()
        val userId = requireNotNull(user.id) { "a link is for a saved user" }
        accountTokens.issue(AccountToken.issue(userId, purpose, tokenService.hashToken(token), clock.instant()))
        return token
    }
}

/** Counts the requests for a link, so that nobody can use the mail as a way to flood an address. */
@Service
class RecoveryRequestGuard(
    private val guard: LoginAttemptGuard,
) {
    /** A request counts as soon as it is made, whatever the account: the limit is on who asks, not on who exists. */
    fun checkAndCount(
        key: String,
        ip: String,
    ) {
        guard.lockedFor(key, ip)?.let { throw TooManyLoginAttemptsException(it) }
        guard.recordFailure(key, ip)
    }
}

/** Confirms the e-mail address with the link that was mailed to it. */
@Observed(name = "dbook.usecase")
@Service
class VerifyEmailUseCase(
    private val accountTokens: AccountTokenRepository,
    private val users: UserRepository,
    private val tokenService: TokenService,
    private val meterRegistry: MeterRegistry,
    private val clock: Clock,
) {
    @Transactional
    fun execute(token: String) {
        val now = clock.instant()
        val link =
            accountTokens.findByTokenHash(tokenService.hashToken(token))
                ?.takeIf { it.purpose == AccountTokenPurpose.EMAIL_VERIFICATION && it.isUsableAt(now) }
        if (link == null || !accountTokens.consume(requireNotNull(link.id), now)) {
            meterRegistry.countOutcome("dbook.account.email_verification", "invalid_link")
            throw InvalidAccountTokenException()
        }
        val user = users.findById(link.userId) ?: throw UserNotFoundException(link.userId)
        users.save(user.verifyEmail(now))
        meterRegistry.countOutcome("dbook.account.email_verification", "verified")
    }
}

/** Mails a new confirmation link to the signed-in customer (the old one stops working). */
@Observed(name = "dbook.usecase")
@Service
class ResendEmailVerificationUseCase(
    private val users: UserRepository,
    private val issuer: AccountLinkIssuer,
    private val mailer: AccountMailer,
    private val guard: RecoveryRequestGuard,
) {
    @Transactional
    fun execute(
        userId: Long,
        clientIp: String,
    ) {
        val user = users.findById(userId) ?: throw UserNotFoundException(userId)
        check(!user.isEmailVerified) { "The e-mail address is already confirmed" }
        guard.checkAndCount("verify:$userId", clientIp)
        val token = issuer.issue(user, AccountTokenPurpose.EMAIL_VERIFICATION)
        afterCommit { mailer.sendVerification(user, token) }
    }
}

/**
 * "I forgot my password": mails a link to choose a new one. The answer is **the same whether the address belongs to
 * an account or not** (and the mail leaves on another thread), so the endpoint cannot be used to find out who has an
 * account. A blocked or anonymized account gets nothing. Staff get the link to the portal, customers to the app.
 */
@Observed(name = "dbook.usecase")
@Service
class RequestPasswordResetUseCase(
    private val users: UserRepository,
    private val issuer: AccountLinkIssuer,
    private val mailer: AccountMailer,
    private val guard: RecoveryRequestGuard,
    private val meterRegistry: MeterRegistry,
) {
    @Transactional
    fun execute(
        email: String,
        clientIp: String,
    ) {
        val normalized = email.trim().lowercase()
        guard.checkAndCount("reset:$normalized", clientIp)
        val user = users.findByEmail(normalized)?.takeIf { !it.isBlocked && !it.isAnonymized }
        if (user == null) {
            meterRegistry.countOutcome("dbook.account.password_reset", "no_account")
            return
        }
        val token = issuer.issue(user, AccountTokenPurpose.PASSWORD_RESET)
        afterCommit { mailer.sendPasswordReset(user, token) }
        meterRegistry.countOutcome("dbook.account.password_reset", "requested")
    }
}

data class ResetPasswordCommand(
    val token: String,
    val newPassword: String,
)

/**
 * Chooses the new password with the link. The link is spent **only when the password is accepted** (a weak one does
 * not burn it), every session of the account ends (whoever had the old password or a stolen session is out), the
 * failed-login counter is cleared, and the address counts as confirmed: the link proved the mailbox.
 */
@Observed(name = "dbook.usecase")
@Service
class ResetPasswordUseCase(
    private val accountTokens: AccountTokenRepository,
    private val users: UserRepository,
    private val tokenService: TokenService,
    private val passwordHasher: PasswordHasher,
    private val refreshTokens: RefreshTokenRepository,
    private val attemptGuard: LoginAttemptGuard,
    private val meterRegistry: MeterRegistry,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: ResetPasswordCommand) {
        val now = clock.instant()
        val link = openLink(command.token, now)
        val user = users.findById(link.userId)?.takeIf { !it.isBlocked && !it.isAnonymized } ?: invalid()
        val minLength = if (user.role.isStaff) PasswordPolicy.STAFF_MIN_LENGTH else PasswordPolicy.CLIENT_MIN_LENGTH
        PasswordPolicy.validate(command.newPassword, user.email, minLength)
        if (!accountTokens.consume(requireNotNull(link.id), now)) {
            invalid()
        }
        users.save(user.withPasswordHash(passwordHasher.hash(command.newPassword)).verifyEmail(now))
        refreshTokens.revokeAllForUser(requireNotNull(user.id))
        attemptGuard.clear(user.email)
        meterRegistry.countOutcome("dbook.account.password_reset", "completed")
    }

    private fun openLink(
        token: String,
        now: Instant,
    ): AccountToken =
        accountTokens.findByTokenHash(tokenService.hashToken(token))
            ?.takeIf { it.purpose == AccountTokenPurpose.PASSWORD_RESET && it.isUsableAt(now) }
            ?: invalid()

    private fun invalid(): Nothing {
        meterRegistry.countOutcome("dbook.account.password_reset", "invalid_link")
        throw InvalidAccountTokenException()
    }
}
