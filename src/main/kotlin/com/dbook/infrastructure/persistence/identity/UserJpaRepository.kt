package com.dbook.infrastructure.persistence.identity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
    fun findByEmail(email: String): UserJpaEntity?

    // touches this one column only, so it cannot undo a block written a moment earlier
    @Modifying
    @Query("UPDATE UserJpaEntity u SET u.lastLoginAt = :at WHERE u.id = :id")
    fun updateLastLoginAt(
        @Param("id") id: Long,
        @Param("at") at: Instant,
    )
}
