package com.dbook.application.identity

import com.dbook.domain.identity.PasswordHasher
import com.dbook.domain.identity.PasswordPolicy
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserAlreadyExistsException
import com.dbook.domain.identity.UserRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class BootstrapSuperAdminCommand(
    val email: String,
    val password: String,
)

/**
 * Creates the first SUPER_ADMIN, and only while there is none: the way into the portal before anyone can invite
 * anyone. It is idempotent, so it can run at every startup.
 */
@Observed(name = "dbook.usecase")
@Service
class BootstrapSuperAdminUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
) {
    /** @return true when the account was created, false when a SUPER_ADMIN already existed. */
    @Transactional
    fun execute(command: BootstrapSuperAdminCommand): Boolean {
        if (userRepository.existsByRole(Role.SUPER_ADMIN)) {
            return false
        }
        val email = command.email.trim().lowercase()
        PasswordPolicy.validate(command.password, email, PasswordPolicy.STAFF_MIN_LENGTH)
        if (userRepository.findByEmail(email) != null) {
            throw UserAlreadyExistsException(email)
        }
        userRepository.save(
            User(
                email = email,
                passwordHash = passwordHasher.hash(command.password),
                name = email.substringBefore("@"),
                role = Role.SUPER_ADMIN,
            ),
        )
        return true
    }
}
