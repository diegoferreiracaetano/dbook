package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.Role
import com.dbook.domain.identity.User
import com.dbook.domain.identity.UserRepository
import com.dbook.domain.identity.UserStatus
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
class UserRepositoryAdapter(
    private val userJpaRepository: UserJpaRepository,
) : UserRepository {
    override fun findById(id: Long): User? = userJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByEmail(email: String): User? = userJpaRepository.findByEmailIgnoreCase(email)?.toDomain()

    override fun save(user: User): User {
        val entity = user.toJpaEntity()
        return userJpaRepository.save(entity).toDomain()
    }

    @Transactional
    override fun recordLogin(
        userId: Long,
        at: Instant,
    ) = userJpaRepository.updateLastLoginAt(userId, at)

    override fun findStaff(): List<User> = userJpaRepository.findByRoleNotOrderById(Role.CLIENT).map { it.toDomain() }

    override fun existsByRole(role: Role): Boolean = userJpaRepository.existsByRole(role)

    // MANDATORY: a lock taken in a transaction of its own would be released before the caller could use it
    @Transactional(propagation = Propagation.MANDATORY)
    override fun lockActiveByRole(role: Role): List<User> =
        userJpaRepository.lockByRoleAndStatus(role, UserStatus.ACTIVE).map { it.toDomain() }
}
