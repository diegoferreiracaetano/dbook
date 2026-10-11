package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.RefreshToken
import com.dbook.domain.identity.RefreshTokenRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class RefreshTokenRepositoryAdapter(
    private val refreshTokenJpaRepository: RefreshTokenJpaRepository,
) : RefreshTokenRepository {
    override fun findByTokenHash(tokenHash: String): RefreshToken? =
        refreshTokenJpaRepository.findByTokenHash(tokenHash)?.toDomain()

    override fun save(refreshToken: RefreshToken): RefreshToken =
        refreshTokenJpaRepository.save(refreshToken.toJpaEntity()).toDomain()

    override fun revoke(id: Long) {
        val entity = refreshTokenJpaRepository.findById(id).orElseThrow()
        entity.revoked = true
        refreshTokenJpaRepository.save(entity)
    }

    @Transactional
    override fun consume(id: Long): Boolean = refreshTokenJpaRepository.consumeById(id) == 1

    @Transactional
    override fun revokeFamily(familyId: String) =
        refreshTokenJpaRepository.revokeAllByFamilyId(java.util.UUID.fromString(familyId))

    @Transactional
    override fun revokeAllForUser(userId: Long) = refreshTokenJpaRepository.revokeAllByUserId(userId)

    override fun findActiveByUser(
        userId: Long,
        now: java.time.Instant,
    ): List<RefreshToken> = refreshTokenJpaRepository.findActiveByUser(userId, now).map { it.toDomain() }
}
