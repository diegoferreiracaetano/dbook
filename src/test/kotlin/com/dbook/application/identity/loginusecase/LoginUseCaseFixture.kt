package com.dbook.application.identity.loginusecase

import com.dbook.application.identity.IssueTokenPairService
import com.dbook.application.identity.LoginAttemptGuard
import com.dbook.application.identity.LoginAttemptsPolicy
import com.dbook.application.identity.LoginCommand
import com.dbook.application.identity.LoginUseCase
import com.dbook.application.identity.SessionAudience
import com.dbook.application.identity.TwoFactorGate
import com.dbook.application.identity.TwoFactorPolicy
import com.dbook.application.identity.twofactor.FakeTwoFactorRepository
import com.dbook.domain.identity.ChallengePurpose
import com.dbook.domain.identity.LoginAttemptLimiter
import com.dbook.domain.identity.PasswordHasher
import com.dbook.domain.identity.RefreshToken
import com.dbook.domain.identity.RefreshTokenRepository
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.TokenService
import com.dbook.domain.identity.TwoFactorChallenge
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FakeTokenService : TokenService {
    override fun generateAccessToken(user: User): String = "access-${user.id}"

    override fun generateRefreshToken(user: User): String = "refresh-${user.id}-${System.nanoTime()}"

    override fun parseUserId(token: String): Long? = null

    override fun parseRole(token: String): Role? = null

    // "challenge-VERIFY-7": readable in a failing assertion, and nothing else is a challenge
    override fun generateChallengeToken(
        userId: Long,
        purpose: ChallengePurpose,
    ): String = "challenge-${purpose.name}-$userId"

    override fun parseChallenge(token: String): TwoFactorChallenge? =
        Regex("^challenge-(\\w+)-(\\d+)$").matchEntire(token)?.destructured?.let { (purpose, userId) ->
            TwoFactorChallenge(userId.toLong(), ChallengePurpose.valueOf(purpose))
        }

    override fun hashToken(token: String): String = "hash:$token"

    override fun refreshTokenExpiresAt(): Instant = Instant.now().plusSeconds(3600)
}

class FakeRefreshTokenRepository : RefreshTokenRepository {
    val saved = mutableListOf<RefreshToken>()
    val revokedForUsers = mutableListOf<Long>()

    override fun findByTokenHash(tokenHash: String): RefreshToken? = saved.find { it.tokenHash == tokenHash }

    override fun save(refreshToken: RefreshToken): RefreshToken {
        val stored =
            RefreshToken(
                saved.size + 1L,
                refreshToken.userId,
                refreshToken.tokenHash,
                refreshToken.expiresAt,
                familyId = refreshToken.familyId,
            )
        saved += stored
        return stored
    }

    override fun revoke(id: Long) {
        val index = saved.indexOfFirst { it.id == id }
        val token = saved[index]
        saved[index] = RefreshToken(token.id, token.userId, token.tokenHash, token.expiresAt, true, token.familyId)
    }

    val familiesRevoked = mutableListOf<String>()

    override fun consume(id: Long): Boolean {
        val token = saved.first { it.id == id }
        if (token.revoked) return false
        revoke(id)
        return true
    }

    override fun revokeFamily(familyId: String) {
        familiesRevoked += familyId
        saved.replaceAll {
            if (it.familyId == familyId) {
                RefreshToken(
                    it.id,
                    it.userId,
                    it.tokenHash,
                    it.expiresAt,
                    true,
                    it.familyId,
                )
            } else {
                it
            }
        }
    }

    override fun revokeAllForUser(userId: Long) {
        revokedForUsers += userId
    }
}

class SingleUserRepository(var user: User) : UserRepository {
    val loginsRecorded = mutableListOf<Pair<Long, Instant>>()

    override fun findById(id: Long): User? = user.takeIf { it.id == id }

    override fun findByEmail(email: String): User? = user.takeIf { it.email == email }

    override fun save(user: User): User {
        this.user = user
        return user
    }

    override fun recordLogin(
        userId: Long,
        at: Instant,
    ) {
        loginsRecorded += userId to at
    }

    override fun findStaff(): List<User> = error("not needed for this test")

    override fun existsByRole(role: Role): Boolean = error("not needed for this test")

    override fun lockActiveByRole(role: Role): List<User> = error("not needed for this test")
}

class FixedPasswordHasher(private val validPassword: String) : PasswordHasher {
    var matchCalls = 0
        private set

    override fun hash(rawPassword: String): String = "hashed:$rawPassword"

    override fun matches(
        rawPassword: String,
        hash: String,
    ): Boolean {
        matchCalls++
        return rawPassword == validPassword
    }
}

class FakeLoginAttemptLimiter : LoginAttemptLimiter {
    private val failures = mutableMapOf<String, Int>()

    fun failuresOf(key: String) = failures[key] ?: 0

    override fun retryAfterSeconds(
        key: String,
        maxFailures: Int,
    ): Long? = if (failuresOf(key) >= maxFailures) RETRY_AFTER_SECONDS else null

    override fun recordFailure(key: String) {
        failures[key] = failuresOf(key) + 1
    }

    override fun clear(key: String) {
        failures.remove(key)
    }

    companion object {
        const val RETRY_AFTER_SECONDS = 600L
    }
}

// Shared "given": one existing user (a CLIENT unless a scenario overrides it) whose correct password is
// "correct-password", a frozen clock, and a limiter that locks an email after 3 failures and an address after 5.
abstract class LoginUseCaseFixture {
    protected open val existingUser =
        User(id = 1, email = "diego@example.com", passwordHash = "irrelevant", name = "Diego", role = Role.CLIENT)

    protected val now: Instant = Instant.parse("2026-10-04T12:00:00Z")
    protected val refreshTokenRepository = FakeRefreshTokenRepository()
    protected val limiter = FakeLoginAttemptLimiter()
    protected val hasher = FixedPasswordHasher(validPassword = "correct-password")
    protected val meterRegistry = SimpleMeterRegistry()
    protected val userRepository by lazy { SingleUserRepository(existingUser) }
    private val issueTokenPairService = IssueTokenPairService(FakeTokenService(), refreshTokenRepository)
    protected val useCase by lazy {
        LoginUseCase(
            userRepository,
            hasher,
            issueTokenPairService,
            LoginAttemptGuard(limiter, LoginAttemptsPolicy(maxFailuresPerEmail = 3, maxFailuresPerIp = 5)),
            TwoFactorGate(FakeTwoFactorRepository(), TwoFactorPolicy(emptySet())),
            meterRegistry,
            Clock.fixed(now, ZoneOffset.UTC),
        )
    }

    protected fun login(
        email: String = "diego@example.com",
        password: String = "correct-password",
        ip: String = "10.0.0.1",
        audience: SessionAudience = SessionAudience.CLIENT,
    ) = useCase.execute(LoginCommand(email, password, ip, audience))

    protected fun count(
        outcome: String,
        audience: String = "client",
    ) = meterRegistry.counter("dbook.auth.login", "outcome", outcome, "audience", audience).count()
}
