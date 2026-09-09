package com.dbook.application

import com.dbook.domain.PasswordHasher
import com.dbook.domain.Role
import com.dbook.domain.User
import com.dbook.domain.UserAlreadyExistsException
import com.dbook.domain.UserRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class FakeUserRepository : UserRepository {
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

private class FakePasswordHasher : PasswordHasher {
    override fun hash(rawPassword: String): String = "hashed:$rawPassword"

    override fun matches(
        rawPassword: String,
        hash: String,
    ): Boolean = hash == "hashed:$rawPassword"
}

class RegisterUserUseCaseTest {
    private val userRepository = FakeUserRepository()
    private val useCase = RegisterUserUseCase(userRepository, FakePasswordHasher())

    @Test
    fun `registers a new user as CLIENT with a hashed password`() {
        val user = useCase.execute(RegisterUserCommand(email = "diego@example.com", password = "s3cret"))

        assertEquals(Role.CLIENT, user.role)
        assertEquals("hashed:s3cret", user.passwordHash)
    }

    @Test
    fun `rejects a duplicate email`() {
        useCase.execute(RegisterUserCommand(email = "diego@example.com", password = "s3cret"))

        assertFailsWith<UserAlreadyExistsException> {
            useCase.execute(RegisterUserCommand(email = "diego@example.com", password = "other"))
        }
    }
}
