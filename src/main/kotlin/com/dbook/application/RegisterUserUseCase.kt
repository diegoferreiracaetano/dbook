package com.dbook.application

import com.dbook.domain.PasswordHasher
import com.dbook.domain.Role
import com.dbook.domain.User
import com.dbook.domain.UserAlreadyExistsException
import com.dbook.domain.UserRepository
import org.springframework.stereotype.Service

data class RegisterUserCommand(
    val email: String,
    val password: String,
)

/** Registers a new [User], always as [Role.CLIENT] — see the inline note on [execute] for why. */
@Service
class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
) {
    // Public registration always creates a CLIENT — there is no way to self-promote
    // to ADMIN through this endpoint. Promoting a user is a manual DB operation for
    // now (documented in the README); no admin-management endpoint was requested.
    fun execute(command: RegisterUserCommand): User {
        if (userRepository.findByEmail(command.email) != null) {
            throw UserAlreadyExistsException(command.email)
        }
        val user =
            User(
                email = command.email,
                passwordHash = passwordHasher.hash(command.password),
                role = Role.CLIENT,
            )
        return userRepository.save(user)
    }
}
