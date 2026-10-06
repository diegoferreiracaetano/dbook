package com.dbook.application.identity

import com.dbook.domain.identity.RefreshTokenRepository
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserNotFoundException
import com.dbook.domain.identity.UserRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

data class BlockUserCommand(
    val userId: Long,
    val reason: String,
)

@Observed(name = "dbook.usecase")
@Service
class BlockUserUseCase(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val clock: Clock,
) {
    // the sessions end in the same transaction: a block that left refresh tokens alive would not take effect
    @Transactional
    fun execute(command: BlockUserCommand): User {
        val user = userRepository.findById(command.userId) ?: throw UserNotFoundException(command.userId)
        val blocked = userRepository.save(user.block(command.reason, clock.instant()))
        refreshTokenRepository.revokeAllForUser(command.userId)
        return blocked
    }
}
