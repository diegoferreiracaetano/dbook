package com.dbook.domain.identity

/**
 * Persistence port for [RefreshToken]. Tokens are stored hashed ([findByTokenHash] takes
 * an already-hashed value) — the raw token itself never touches the database.
 */
interface RefreshTokenRepository {
    fun findByTokenHash(tokenHash: String): RefreshToken?

    fun save(refreshToken: RefreshToken): RefreshToken

    /** Marks a token as used (logout). */
    fun revoke(id: Long)

    /**
     * Spends a token **atomically**: true for the one caller that found it unspent. Two refreshes of the same token at
     * once cannot both win, so the second is recognized as a reuse instead of issuing a second child.
     */
    fun consume(id: Long): Boolean

    /** Revokes every token of the family (a spent token was presented again: a stolen copy). */
    fun revokeFamily(familyId: String)

    fun revokeAllForUser(userId: Long)

    /** The live token of each session of the user (a session has one: the last it was rotated to), newest first. */
    fun findActiveByUser(
        userId: Long,
        now: java.time.Instant,
    ): List<RefreshToken>
}
