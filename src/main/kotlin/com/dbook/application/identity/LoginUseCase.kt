package com.dbook.application.identity

import com.dbook.application.common.countOutcome
import com.dbook.domain.identity.AccountBlockedException
import com.dbook.domain.identity.InvalidCredentialsException
import com.dbook.domain.identity.PasswordHasher
import com.dbook.domain.identity.TooManyLoginAttemptsException
import com.dbook.domain.identity.UserRepository
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import java.time.Clock

data class LoginCommand(
    val email: String,
    val password: String,
    val clientIp: String,
    val audience: SessionAudience = SessionAudience.CLIENT,
)

@Observed(name = "dbook.usecase")
@Service
class LoginUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val issueTokenPairService: IssueTokenPairService,
    private val attemptGuard: LoginAttemptGuard,
    private val meterRegistry: MeterRegistry,
    private val clock: Clock,
) {
    // BCrypt is slow on purpose: checking an unknown email against a throwaway hash makes it take
    // as long as a wrong password, so the response time does not say which emails are registered
    private val unknownUserHash by lazy { passwordHasher.hash("unknown-user") }

    fun execute(command: LoginCommand): TokenPair {
        attemptGuard.lockedFor(command.email, command.clientIp)?.let { retryAfter ->
            countOutcome(command, "rate_limited")
            throw TooManyLoginAttemptsException(retryAfter)
        }
        val user = userRepository.findByEmail(command.email)
        val passwordMatches = passwordHasher.matches(command.password, user?.passwordHash ?: unknownUserHash)
        if (user == null || !passwordMatches || !command.audience.accepts(user.role)) {
            rejectCredentials(command)
        }
        // checked only after the credentials were right: a blocked account is not revealed to a guesser
        if (user.isBlocked) {
            countOutcome(command, "blocked")
            throw AccountBlockedException()
        }
        attemptGuard.clear(command.email)
        userRepository.recordLogin(requireNotNull(user.id), clock.instant())
        countOutcome(command, "success")
        return issueTokenPairService.issueFor(user)
    }

    private fun rejectCredentials(command: LoginCommand): Nothing {
        attemptGuard.recordFailure(command.email, command.clientIp)
        countOutcome(command, "invalid_credentials")
        throw InvalidCredentialsException()
    }

    private fun countOutcome(
        command: LoginCommand,
        outcome: String,
    ) = meterRegistry.countOutcome("dbook.auth.login", outcome, "audience", command.audience.tag)
}
