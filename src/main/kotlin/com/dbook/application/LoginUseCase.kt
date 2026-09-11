package com.dbook.application

import com.dbook.domain.InvalidCredentialsException
import com.dbook.domain.PasswordHasher
import com.dbook.domain.UserRepository
import org.springframework.stereotype.Service

data class LoginCommand(
    val email: String,
    val password: String,
)

/** Authenticates by email/password and issues a fresh access/refresh token pair. */
@Service
class LoginUseCase(
    private val userRepository: UserRepository,
    private val passwordHasher: PasswordHasher,
    private val issueTokenPairService: IssueTokenPairService,
) {
    fun execute(command: LoginCommand): TokenPair {
        val user = userRepository.findByEmail(command.email) ?: throw InvalidCredentialsException()
        if (!passwordHasher.matches(command.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }
        return issueTokenPairService.issueFor(user)
    }
}
