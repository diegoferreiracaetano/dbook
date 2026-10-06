package com.dbook.application.identity.twofactor

import com.dbook.MutableClock
import com.dbook.application.audit.FakeAuditLog
import com.dbook.application.identity.ChallengeResolver
import com.dbook.application.identity.ConfirmTotpUseCase
import com.dbook.application.identity.ConfirmTwoFactorEnrollmentAtLoginUseCase
import com.dbook.application.identity.DisableTwoFactorUseCase
import com.dbook.application.identity.EnrollTotpUseCase
import com.dbook.application.identity.IssueTokenPairService
import com.dbook.application.identity.LoginAttemptGuard
import com.dbook.application.identity.LoginAttemptsPolicy
import com.dbook.application.identity.LoginCommand
import com.dbook.application.identity.LoginUseCase
import com.dbook.application.identity.PasswordConfirmation
import com.dbook.application.identity.ResetTwoFactorUseCase
import com.dbook.application.identity.SecondFactorAttempts
import com.dbook.application.identity.SecondFactorVerifier
import com.dbook.application.identity.SessionAudience
import com.dbook.application.identity.StaffLoginOutcome
import com.dbook.application.identity.StaffLoginUseCase
import com.dbook.application.identity.StaffSafeguards
import com.dbook.application.identity.StartTwoFactorEnrollmentAtLoginUseCase
import com.dbook.application.identity.TwoFactorGate
import com.dbook.application.identity.TwoFactorPolicy
import com.dbook.application.identity.VerifyTwoFactorCommand
import com.dbook.application.identity.VerifyTwoFactorLoginUseCase
import com.dbook.application.identity.loginusecase.FakeLoginAttemptLimiter
import com.dbook.application.identity.loginusecase.FakeRefreshTokenRepository
import com.dbook.application.identity.loginusecase.FakeTokenService
import com.dbook.application.identity.loginusecase.FixedPasswordHasher
import com.dbook.application.identity.staff.InMemoryUserRepository
import com.dbook.domain.common.access.Actor
import com.dbook.domain.common.access.Role
import com.dbook.domain.identity.RecoveryCodeGenerator
import com.dbook.domain.identity.TotpEnrollment
import com.dbook.domain.identity.TwoFactorRepository
import com.dbook.domain.identity.User
import com.dbook.infrastructure.security.AesGcmSecretCipher
import com.dbook.infrastructure.security.Rfc6238TotpService
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.time.Duration
import java.time.Instant

// The same rules as the database's conditional UPDATEs: only a newer step advances, only an unspent code is spent.
class FakeTwoFactorRepository : TwoFactorRepository {
    private val enrollments = mutableMapOf<Long, TotpEnrollment>()
    private val codes = mutableMapOf<Long, MutableMap<String, Boolean>>() // hash -> spent

    override fun find(userId: Long): TotpEnrollment? = enrollments[userId]

    override fun savePending(
        userId: Long,
        secretEncrypted: String,
    ) {
        if (enrollments[userId]?.isActive != true) {
            enrollments[userId] = TotpEnrollment(userId, secretEncrypted, null, 0)
        }
    }

    override fun confirm(
        userId: Long,
        step: Long,
        now: Instant,
        recoveryCodeHashes: List<String>,
    ): Boolean {
        val pending = enrollments[userId]?.takeIf { !it.isActive } ?: return false
        enrollments[userId] = pending.copy(confirmedAt = now, lastUsedStep = step)
        codes[userId] = recoveryCodeHashes.associateWith { false }.toMutableMap()
        return true
    }

    override fun advanceStep(
        userId: Long,
        step: Long,
    ): Boolean {
        val current = enrollments[userId]?.takeIf { it.isActive && it.lastUsedStep < step } ?: return false
        enrollments[userId] = current.copy(lastUsedStep = step)
        return true
    }

    override fun spendRecoveryCode(
        userId: Long,
        hash: String,
        now: Instant,
    ): Boolean {
        val mine = codes[userId]
        if (mine?.get(hash) != false) return false
        mine[hash] = true
        return true
    }

    override fun unspentRecoveryCodes(userId: Long): Int = codes[userId]?.count { !it.value } ?: 0

    override fun remove(userId: Long) {
        enrollments.remove(userId)
        codes.remove(userId)
    }
}

class SequentialRecoveryCodes : RecoveryCodeGenerator {
    private var next = 0

    override fun generate(): String = "CODE${next++}-ABCDE"
}

// Shared "given": a SUPER_ADMIN (id 1) with the password "correct-password", another one (id 2), a customer (id 3),
// the real RFC 6238 service and the real AES-GCM cipher (they are the thing under test too), and a clock that moves
// by hand. A "phone" is the authenticator: it makes the code the real service would accept at the clock's time.
abstract class TwoFactorFixture {
    protected val clock = MutableClock(Instant.parse("2026-10-05T12:00:00Z"))
    protected val root =
        User(id = 1, email = "root@example.com", passwordHash = "h", name = "Root", role = Role.SUPER_ADMIN)
    protected val other =
        User(id = 2, email = "other@example.com", passwordHash = "h", name = "Other", role = Role.SUPER_ADMIN)
    protected val customer = User(id = 3, email = "customer@example.com", passwordHash = "h", name = "Customer")
    protected val rootActor = Actor(1, Role.SUPER_ADMIN)

    protected open val requiredRoles: Set<Role> = emptySet()

    protected val users = InMemoryUserRepository(root, other, customer)
    protected val repository = FakeTwoFactorRepository()
    protected val audit = FakeAuditLog()
    protected val refreshTokens = FakeRefreshTokenRepository()
    protected val totp = Rfc6238TotpService("DBook")
    protected val cipher = AesGcmSecretCipher("a-long-random-text-for-the-tests-0123456789")
    protected val meters = SimpleMeterRegistry()
    protected val limiter = FakeLoginAttemptLimiter()

    private val tokens = FakeTokenService()
    private val hasher = FixedPasswordHasher(validPassword = "correct-password")
    private val guard = LoginAttemptGuard(limiter, LoginAttemptsPolicy(maxFailuresPerEmail = 3, maxFailuresPerIp = 5))
    private val gate by lazy { TwoFactorGate(repository, TwoFactorPolicy(requiredRoles)) }
    private val verifier by lazy { SecondFactorVerifier(repository, totp, cipher, tokens, clock) }
    private val attempts = SecondFactorAttempts(guard)
    private val challenges = ChallengeResolver(tokens, users)
    private val login by lazy {
        LoginUseCase(users, hasher, IssueTokenPairService(tokens, refreshTokens), guard, gate, meters, clock)
    }

    protected val staffLogin by lazy { StaffLoginUseCase(login, gate, tokens, meters) }
    protected val enroll by lazy { EnrollTotpUseCase(users, repository, totp, cipher) }
    protected val confirm by lazy {
        ConfirmTotpUseCase(repository, totp, cipher, SequentialRecoveryCodes(), tokens, audit, clock)
    }
    protected val verify by lazy { VerifyTwoFactorLoginUseCase(challenges, login, verifier, attempts) }
    protected val startEnrollmentAtLogin by lazy { StartTwoFactorEnrollmentAtLoginUseCase(challenges, enroll) }
    protected val confirmEnrollmentAtLogin by lazy {
        ConfirmTwoFactorEnrollmentAtLoginUseCase(challenges, confirm, login, attempts)
    }
    protected val disable by lazy {
        DisableTwoFactorUseCase(users, repository, gate, PasswordConfirmation(hasher, guard), verifier, audit)
    }
    protected val reset by lazy {
        ResetTwoFactorUseCase(StaffSafeguards(users), repository, refreshTokens, audit)
    }
    protected val clientLogin by lazy { login }

    /** What the authenticator shows for this account right now. */
    protected fun codeOnThePhone(userId: Long = 1): String {
        val secret = cipher.decrypt(requireNotNull(repository.find(userId)).secretEncrypted)
        return totp.codeAt(secret, clock.instant().epochSecond / STEP_SECONDS)
    }

    /** Time passes until the authenticator shows a different code. */
    protected fun nextThirtySeconds() = clock.advance(Duration.ofSeconds(STEP_SECONDS))

    /** Enrolls [userId] fully (the authenticator confirms its first code): the recovery codes it was shown. */
    protected fun enrolled(userId: Long = 1): List<String> {
        enroll.execute(userId)
        val codes = confirm.execute(Actor(userId, Role.SUPER_ADMIN), codeOnThePhone(userId))
        nextThirtySeconds() // the confirming code is spent: the next one belongs to a later step
        return codes
    }

    protected fun staffPassword(email: String = "root@example.com") =
        LoginCommand(email, "correct-password", "10.0.0.1", SessionAudience.STAFF)

    protected fun challengeOf(outcome: StaffLoginOutcome): String =
        (outcome as StaffLoginOutcome.Challenge).challengeToken

    protected fun verifyCommand(
        challenge: String,
        code: String,
        ip: String = "10.0.0.1",
    ) = VerifyTwoFactorCommand(challenge, code, ip)

    protected companion object {
        const val STEP_SECONDS = 30L
    }
}
