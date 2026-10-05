package com.dbook.application.crm

import com.dbook.application.identity.PasswordConfirmation
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.UserNotFoundException
import com.dbook.domain.identity.UserRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class DeleteOwnAccountCommand(
    val userId: Long,
    val password: String,
    val clientIp: String,
)

// The customer's own right to be forgotten. The password proves it is them: a stolen access token must not be
// enough to destroy an account.
@Observed(name = "dbook.usecase")
@Service
class DeleteOwnAccountUseCase(
    private val userRepository: UserRepository,
    private val passwordConfirmation: PasswordConfirmation,
    private val customerAnonymizer: CustomerAnonymizer,
) {
    @Transactional
    fun execute(command: DeleteOwnAccountCommand) {
        val user = userRepository.findById(command.userId) ?: throw UserNotFoundException(command.userId)
        check(user.role.permissions.isEmpty()) { "A staff account is closed by blocking it, not by deleting it" }
        passwordConfirmation.confirm(user, command.password, command.clientIp)
        customerAnonymizer.anonymize(Actor(command.userId, user.role), user, "Requested by the customer")
    }
}
