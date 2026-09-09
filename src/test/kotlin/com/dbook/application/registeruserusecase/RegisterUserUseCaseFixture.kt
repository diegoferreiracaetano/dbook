package com.dbook.application.registeruserusecase

import com.dbook.application.RegisterUserUseCase
import com.dbook.domain.PasswordHasher
import com.dbook.domain.User
import com.dbook.domain.UserRepository

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
                role = user.role,
            )
        users += saved
        return saved
    }
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
