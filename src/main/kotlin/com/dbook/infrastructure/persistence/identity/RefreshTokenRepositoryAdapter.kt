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
    override fun revokeAllForUser(userId: Long) = refreshTokenJpaRepository.revokeAllByUserId(userId)
}
