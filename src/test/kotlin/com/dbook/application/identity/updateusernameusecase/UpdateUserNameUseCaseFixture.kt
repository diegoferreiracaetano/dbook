package com.dbook.application.identity.updateusernameusecase

import com.dbook.application.identity.UpdateUserNameUseCase
import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import java.time.Instant

class FakeUserRepository : UserRepository {
    val users = mutableListOf<User>()

    override fun findById(id: Long): User? = users.find { it.id == id }

    override fun findByEmail(email: String): User? = users.find { it.email == email }

    override fun save(user: User): User {
        users.removeIf { it.id == user.id }
        users += user
        return user
    }

    override fun recordLogin(
        userId: Long,
        at: Instant,
    ) = error("not needed for this test")

    override fun findStaff(): List<User> = error("not needed for this test")

    override fun existsByRole(role: Role): Boolean = error("not needed for this test")

    override fun lockActiveByRole(role: Role): List<User> = error("not needed for this test")
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
