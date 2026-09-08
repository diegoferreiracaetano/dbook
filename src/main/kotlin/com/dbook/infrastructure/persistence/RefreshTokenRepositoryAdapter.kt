package com.dbook.infrastructure.persistence

import com.dbook.domain.RefreshToken
import com.dbook.domain.RefreshTokenRepository
import org.springframework.stereotype.Repository

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
}
