package com.dbook.application.identity

import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserNotFoundException
import com.dbook.domain.identity.UserRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

data class UpdateUserNameCommand(
    val userId: Long,
    val name: String,
)

/** Updates a [User]'s display name — the only self-editable profile field today. */
@Observed(name = "dbook.usecase")
@Service
class UpdateUserNameUseCase(
    private val userRepository: UserRepository,
) {
    fun execute(command: UpdateUserNameCommand): User {
        val user = userRepository.findById(command.userId) ?: throw UserNotFoundException(command.userId)
        val updated =
            User(
                id = user.id,
                email = user.email,
                passwordHash = user.passwordHash,
                name = command.name,
                role = user.role,
            )
        return userRepository.save(updated)
    }
}
