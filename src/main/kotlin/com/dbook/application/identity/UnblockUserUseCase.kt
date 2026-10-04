package com.dbook.application.identity

import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserNotFoundException
import com.dbook.domain.identity.UserRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

@Observed(name = "dbook.usecase")
@Service
class UnblockUserUseCase(
    private val userRepository: UserRepository,
) {
    fun execute(userId: Long): User {
        val user = userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        return userRepository.save(user.unblock())
    }
}
