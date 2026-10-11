package com.dbook.infrastructure.persistence.identity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenJpaEntity, Long> {
    fun findByTokenHash(tokenHash: String): RefreshTokenJpaEntity?

    @Query(
        "SELECT t FROM RefreshTokenJpaEntity t WHERE t.userId = :userId AND t.revoked = false " +
            "AND t.expiresAt > :now ORDER BY t.id DESC",
    )
    fun findActiveByUser(
        @Param("userId") userId: Long,
        @Param("now") now: java.time.Instant,
    ): List<RefreshTokenJpaEntity>

    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity t SET t.revoked = true WHERE t.id = :id AND t.revoked = false")
    fun consumeById(
        @Param("id") id: Long,
    ): Int

    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity t SET t.revoked = true WHERE t.familyId = :familyId AND t.revoked = false")
    fun revokeAllByFamilyId(
        @Param("familyId") familyId: java.util.UUID,
    )

    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity t SET t.revoked = true WHERE t.userId = :userId AND t.revoked = false")
    fun revokeAllByUserId(
        @Param("userId") userId: Long,
    )
}
