package com.dbook.application.identity.registeruserusecase

import com.dbook.application.identity.RegisterUserUseCase
import com.dbook.domain.identity.PasswordHasher
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import java.time.Instant

class FakeUserRepository : UserRepository {
    val users = mutableListOf<User>()

    override fun findById(id: Long): User? = users.find { it.id == id }

    override fun findByEmail(email: String): User? = users.find { it.email == email }

    override fun save(user: User): User {
        val saved =
            User(
                id = user.id ?: (users.size + 1L),
                email = user.email,
                passwordHash = user.passwordHash,
                name = user.name,
                role = user.role,
            )
        users += saved
        return saved
    }

    override fun recordLogin(
        userId: Long,
        at: Instant,
    ) = error("not needed for this test")

    override fun findStaff(): List<User> = error("not needed for this test")

    override fun existsByRole(role: Role): Boolean = error("not needed for this test")

    override fun lockActiveByRole(role: Role): List<User> = error("not needed for this test")
}

class FakePasswordHasher : PasswordHasher {
    override fun hash(rawPassword: String): String = "hashed:$rawPassword"

    override fun matches(
        rawPassword: String,
        hash: String,
    ): Boolean = hash == "hashed:$rawPassword"
}

abstract class RegisterUserUseCaseFixture {
    protected val userRepository = FakeUserRepository()
    protected val useCase = RegisterUserUseCase(userRepository, FakePasswordHasher())
}
