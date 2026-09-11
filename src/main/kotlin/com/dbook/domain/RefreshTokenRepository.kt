package com.dbook.domain

/**
 * Persistence port for [RefreshToken]. Tokens are stored hashed ([findByTokenHash] takes
 * an already-hashed value) — the raw token itself never touches the database.
 */
interface RefreshTokenRepository {
    fun findByTokenHash(tokenHash: String): RefreshToken?

    fun save(refreshToken: RefreshToken): RefreshToken

    /** Marks a token as used, enforcing single-use rotation. */
    fun revoke(id: Long)
}
