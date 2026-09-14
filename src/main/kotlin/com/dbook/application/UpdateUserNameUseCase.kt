package com.dbook.application

import com.dbook.domain.User
import com.dbook.domain.UserNotFoundException
import com.dbook.domain.UserRepository
import org.springframework.stereotype.Service

data class UpdateUserNameCommand(
    val userId: Long,
    val name: String,
)

/** Updates a [User]'s display name — the only self-editable profile field today. */
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
