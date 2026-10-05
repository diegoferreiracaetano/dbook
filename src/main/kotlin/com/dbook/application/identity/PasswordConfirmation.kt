package com.dbook.application.identity

import com.dbook.domain.identity.InvalidCredentialsException
import com.dbook.domain.identity.PasswordHasher
import com.dbook.domain.identity.TooManyLoginAttemptsException
import com.dbook.domain.identity.User
import org.springframework.stereotype.Service

// "Type your password again" for actions that must come from the account's owner. A wrong password counts against
// the same limit as a wrong login: a stolen access token must not become a way to guess the password.
@Service
class PasswordConfirmation(
    private val passwordHasher: PasswordHasher,
    private val attemptGuard: LoginAttemptGuard,
) {
    fun confirm(
        user: User,
        password: String,
        clientIp: String,
    ) {
        attemptGuard.lockedFor(user.email, clientIp)?.let { throw TooManyLoginAttemptsException(it) }
        if (!passwordHasher.matches(password, user.passwordHash)) {
            attemptGuard.recordFailure(user.email, clientIp)
            throw InvalidCredentialsException()
        }
        attemptGuard.clear(user.email)
    }
}
