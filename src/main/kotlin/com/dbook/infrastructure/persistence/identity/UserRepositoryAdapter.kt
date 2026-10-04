package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

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

    @Transactional
    override fun recordLogin(
        userId: Long,
        at: Instant,
    ) = userJpaRepository.updateLastLoginAt(userId, at)
}
