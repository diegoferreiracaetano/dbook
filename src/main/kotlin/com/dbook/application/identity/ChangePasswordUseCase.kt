package com.dbook.application.identity

import com.dbook.domain.identity.InvalidCredentialsException
import com.dbook.domain.identity.PasswordHasher
import com.dbook.domain.identity.PasswordPolicy
import com.dbook.domain.identity.RefreshTokenRepository
import com.dbook.domain.identity.TooManyLoginAttemptsException
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserNotFoundException
import com.dbook.domain.identity.UserRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class ChangePasswordCommand(
    val userId: Long,
    val currentPassword: String,
    val newPassword: String,
    val clientIp: String,
)

/**
 * Changes the caller's own password and ends every session, so a stolen one stops working. The current password
 * is required, and a wrong one counts against the same limit as a wrong login (a stolen access token must not
 * become a way to guess the password).
 */
@Observed(name = "dbook.usecase")
@Service
class ChangePasswordUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val attemptGuard: LoginAttemptGuard,
) {
    @Transactional
    fun execute(command: ChangePasswordCommand) {
        val user = userRepository.findById(command.userId) ?: throw UserNotFoundException(command.userId)
        requireCurrentPassword(user, command)
        val minLength = if (user.role.isStaff) PasswordPolicy.STAFF_MIN_LENGTH else PasswordPolicy.CLIENT_MIN_LENGTH
        PasswordPolicy.validate(command.newPassword, user.email, minLength)

        userRepository.save(user.withPasswordHash(passwordHasher.hash(command.newPassword)))
        refreshTokenRepository.revokeAllForUser(command.userId)
    }

    private fun requireCurrentPassword(
        user: User,
        command: ChangePasswordCommand,
    ) {
        attemptGuard.lockedFor(user.email, command.clientIp)?.let { throw TooManyLoginAttemptsException(it) }
        if (!passwordHasher.matches(command.currentPassword, user.passwordHash)) {
            attemptGuard.recordFailure(user.email, command.clientIp)
            throw InvalidCredentialsException()
        }
        attemptGuard.clear(user.email)
    }
}
