package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.User
import kotlin.test.Test
import kotlin.test.assertEquals

class FindingAUserByEmailIgnoresCaseTest : UserRepositoryAdapterFixture() {
    @Test
    fun `given a user registered with capitals when looked up in lowercase or uppercase then it is found`() {
        val id =
            requireNotNull(
                userRepository.save(
                    User(email = "Mixed${(1..999_999_999).random()}@Example.com", passwordHash = "h", name = "Mixed"),
                ).id,
            )
        val stored = requireNotNull(userRepository.findById(id)).email

        assertEquals(id, userRepository.findByEmail(stored.lowercase())?.id)
        assertEquals(id, userRepository.findByEmail(stored.uppercase())?.id)
    }
}
