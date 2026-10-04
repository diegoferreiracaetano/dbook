package com.dbook.infrastructure.persistence.identity

import com.dbook.AbstractIntegrationTest
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import org.springframework.beans.factory.annotation.Autowired

abstract class UserRepositoryAdapterFixture : AbstractIntegrationTest() {
    @Autowired
    lateinit var userRepository: UserRepository

    protected fun newUser(): User =
        userRepository.save(
            User(email = "adapter${(1..999_999_999).random()}@example.com", passwordHash = "h", name = "Adapter"),
        )
}
