package com.dbook.infrastructure.persistence.identity

import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenJpaEntity, Long> {
    fun findByTokenHash(tokenHash: String): RefreshTokenJpaEntity?
}
