package com.dbook.application.updateusernameusecase

import com.dbook.application.UpdateUserNameUseCase
import com.dbook.domain.Role
import com.dbook.domain.User
import com.dbook.domain.UserRepository

class FakeUserRepository : UserRepository {
    val users = mutableListOf<User>()

    override fun findById(id: Long): User? = users.find { it.id == id }

    override fun findByEmail(email: String): User? = users.find { it.email == email }

    override fun save(user: User): User {
        users.removeIf { it.id == user.id }
        users += user
        return user
    }
}

abstract class UpdateUserNameUseCaseFixture {
    protected val userRepository = FakeUserRepository()
    protected val useCase = UpdateUserNameUseCase(userRepository)

    protected val existingUser =
        User(
            id = 1,
            email = "diego@example.com",
            passwordHash = "hashed",
            name = "Old Name",
            role = Role.CLIENT,
        ).also { userRepository.users += it }
}
