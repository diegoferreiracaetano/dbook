package com.dbook.domain

interface RefreshTokenRepository {
    fun findByTokenHash(tokenHash: String): RefreshToken?

    fun save(refreshToken: RefreshToken): RefreshToken

    fun revoke(id: Long)
}
