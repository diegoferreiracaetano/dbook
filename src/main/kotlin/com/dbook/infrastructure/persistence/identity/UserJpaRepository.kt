package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.Role
import com.dbook.domain.identity.UserStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
    fun findByEmailIgnoreCase(email: String): UserJpaEntity?

    fun findByRoleNotOrderById(role: Role): List<UserJpaEntity>

    fun existsByRole(role: Role): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserJpaEntity u WHERE u.role = :role AND u.status = :status ORDER BY u.id")
    fun lockByRoleAndStatus(
        @Param("role") role: Role,
        @Param("status") status: UserStatus,
    ): List<UserJpaEntity>

    // touches this one column only, so it cannot undo a block written a moment earlier
    @Modifying
    @Query("UPDATE UserJpaEntity u SET u.lastLoginAt = :at WHERE u.id = :id")
    fun updateLastLoginAt(
        @Param("id") id: Long,
        @Param("at") at: Instant,
    )
}
