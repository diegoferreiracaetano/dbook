package com.dbook.application.identity

import com.dbook.application.common.countOutcome
import com.dbook.domain.identity.AccountBlockedException
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.ChallengePurpose
import com.dbook.domain.identity.InvalidTokenException
import com.dbook.domain.identity.InvalidTwoFactorCodeException
import com.dbook.domain.identity.TokenService
import com.dbook.domain.identity.TooManyLoginAttemptsException
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

/** What the portal's sign-in answers: the session, or what must happen before there is one. */
sealed interface StaffLoginOutcome {
    data class Session(val tokens: TokenPair) : StaffLoginOutcome

    /** The password was right: [challengeToken] goes back with the code, or with the enrollment if required. */
    data class Challenge(
        val challengeToken: String,
        val enrollmentRequired: Boolean,
    ) : StaffLoginOutcome
}

/** The portal's sign-in: the password first, then, for an account with a second factor, the code. */
@Observed(name = "dbook.usecase")
@Service
class StaffLoginUseCase(
    private val loginUseCase: LoginUseCase,
    private val gate: TwoFactorGate,
    private val tokenService: TokenService,
    private val meterRegistry: MeterRegistry,
) {
    fun execute(command: LoginCommand): StaffLoginOutcome {
        val user = loginUseCase.authenticate(command)
        val userId = requireNotNull(user.id)
        return when {
            gate.isEnabled(userId) -> challenge(userId, ChallengePurpose.VERIFY, command)
            gate.isRequiredFor(user.role) -> challenge(userId, ChallengePurpose.ENROLL, command)
            else -> StaffLoginOutcome.Session(loginUseCase.complete(user, command))
        }
    }

    private fun challenge(
        userId: Long,
        purpose: ChallengePurpose,
        command: LoginCommand,
    ): StaffLoginOutcome.Challenge {
        meterRegistry.countOutcome(
            "dbook.auth.login",
            "second_factor_${purpose.name.lowercase()}",
            "audience",
            command.audience.tag,
        )
        return StaffLoginOutcome.Challenge(
            tokenService.generateChallengeToken(userId, purpose),
            enrollmentRequired = purpose == ChallengePurpose.ENROLL,
        )
    }
}

data class VerifyTwoFactorCommand(
    val challengeToken: String,
    val code: String,
    val clientIp: String,
)

/**
 * The second step: the challenge token from the password step plus a code, for the session. Wrong codes count
 * against a limit of their own (per account and per address), so six digits cannot be guessed at leisure.
 */
@Observed(name = "dbook.usecase")
@Service
class VerifyTwoFactorLoginUseCase(
    private val challenges: ChallengeResolver,
    private val loginUseCase: LoginUseCase,
    private val verifier: SecondFactorVerifier,
    private val attempts: SecondFactorAttempts,
) {
    fun execute(command: VerifyTwoFactorCommand): TokenPair {
        val user = challenges.userOf(command.challengeToken, ChallengePurpose.VERIFY)
        val userId = requireNotNull(user.id)
        attempts.requireNotLocked(userId, command.clientIp)
        if (!verifier.accepts(userId, command.code)) {
            attempts.failed(userId, command.clientIp)
            throw InvalidTwoFactorCodeException()
        }
        attempts.succeeded(userId)
        return loginUseCase.complete(user, loginCommandOf(user, command.clientIp))
    }
}

/** What a forced enrollment at sign-in returns: the session, and the recovery codes (shown once). */
data class EnrollmentSession(
    val tokens: TokenPair,
    val recoveryCodes: List<String>,
)

/** Where the role requires a second factor and there is none: the sign-in turns into its enrollment. */
@Observed(name = "dbook.usecase")
@Service
class StartTwoFactorEnrollmentAtLoginUseCase(
    private val challenges: ChallengeResolver,
    private val enroll: EnrollTotpUseCase,
) {
    fun execute(challengeToken: String): TotpEnrollmentStart =
        enroll.execute(requireNotNull(challenges.userOf(challengeToken, ChallengePurpose.ENROLL).id))
}

@Observed(name = "dbook.usecase")
@Service
class ConfirmTwoFactorEnrollmentAtLoginUseCase(
    private val challenges: ChallengeResolver,
    private val confirm: ConfirmTotpUseCase,
    private val loginUseCase: LoginUseCase,
    private val attempts: SecondFactorAttempts,
) {
    fun execute(command: VerifyTwoFactorCommand): EnrollmentSession {
        val user = challenges.userOf(command.challengeToken, ChallengePurpose.ENROLL)
        val userId = requireNotNull(user.id)
        attempts.requireNotLocked(userId, command.clientIp)
        val codes =
            try {
                confirm.execute(Actor(userId, user.role), command.code)
            } catch (ex: InvalidTwoFactorCodeException) {
                attempts.failed(userId, command.clientIp)
                throw ex
            }
        attempts.succeeded(userId)
        return EnrollmentSession(loginUseCase.complete(user, loginCommandOf(user, command.clientIp)), codes)
    }
}

private fun loginCommandOf(
    user: User,
    clientIp: String,
) = LoginCommand(user.email, "", clientIp, SessionAudience.STAFF)

/** Turns a challenge token into the staff account it is for: a bad token, a wrong purpose or a block is refused. */
@Service
class ChallengeResolver(
    private val tokenService: TokenService,
    private val users: UserRepository,
) {
    fun userOf(
        challengeToken: String,
        purpose: ChallengePurpose,
    ): User {
        val challenge = tokenService.parseChallenge(challengeToken)?.takeIf { it.purpose == purpose }
        val user =
            challenge?.let { users.findById(it.userId) }?.takeIf { it.role.isStaff }
                ?: throw InvalidTokenException()
        if (user.isBlocked) {
            throw AccountBlockedException()
        }
        return user
    }
}

/** The wrong codes of the second step, counted per account and per address, with the login's own limiter. */
@Service
class SecondFactorAttempts(
    private val guard: LoginAttemptGuard,
) {
    fun requireNotLocked(
        userId: Long,
        ip: String,
    ) {
        guard.lockedFor(key(userId), ip)?.let { throw TooManyLoginAttemptsException(it) }
    }

    fun failed(
        userId: Long,
        ip: String,
    ) = guard.recordFailure(key(userId), ip)

    fun succeeded(userId: Long) = guard.clear(key(userId))

    private fun key(userId: Long) = "2fa:$userId"
}
