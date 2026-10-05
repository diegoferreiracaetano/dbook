package com.dbook.application.identity

import com.dbook.domain.identity.AnonymizedEmailRepository
import com.dbook.domain.identity.PasswordHasher
import com.dbook.domain.identity.PasswordPolicy
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserAlreadyExistsException
import com.dbook.domain.identity.UserRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

data class RegisterUserCommand(
    val email: String,
    val password: String,
    val name: String,
)

/** Registers a new [User], always as [Role.CLIENT] — see the inline note on [execute] for why. */
@Observed(name = "dbook.usecase")
@Service
class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val anonymizedEmails: AnonymizedEmailRepository,
) {
    // Public registration always creates a CLIENT — there is no way to self-promote
    // to a staff through this endpoint. Promoting a user is a manual DB operation for
    // now (documented in the README); no admin-management endpoint was requested.
    fun execute(command: RegisterUserCommand): User {
        PasswordPolicy.validate(command.password, command.email)

        // an anonymized account's address is never reused: deleting a blocked account must not clear its record
        if (userRepository.findByEmail(command.email) != null || anonymizedEmails.isRemembered(command.email)) {
            throw UserAlreadyExistsException(command.email)
        }
        val user =
            User(
                email = command.email,
                passwordHash = passwordHasher.hash(command.password),
                name = command.name,
                role = Role.CLIENT,
            )
        return userRepository.save(user)
    }
}
