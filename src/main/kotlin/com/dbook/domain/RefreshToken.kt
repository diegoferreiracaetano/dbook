package com.dbook.domain

import java.time.Instant

/**
 * A single-use, rotating refresh token. Stored only as [tokenHash] — the raw token
 * itself is never persisted, only handed to the client once at issuance.
 */
class RefreshToken(
    val id: Long? = null,
    val userId: Long,
    val tokenHash: String,
    val expiresAt: Instant,
    val revoked: Boolean = false,
) {
    fun isValid(now: Instant): Boolean = !revoked && expiresAt.isAfter(now)
}
