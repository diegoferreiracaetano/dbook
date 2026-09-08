package com.dbook.infrastructure.persistence

import com.dbook.domain.User
import com.dbook.domain.UserRepository
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryAdapter(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {
    override fun findById(id: Long): User? = userJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByEmail(email: String): User? = userJpaRepository.findByEmail(email)?.toDomain()

    override fun save(user: User): User {
        val entity = user.toJpaEntity()
        return userJpaRepository.save(entity).toDomain()
    }
}
