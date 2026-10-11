package com.dbook.domain.identity

import java.time.Instant
import java.util.UUID

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
    // the sign-in this token descends from: a spent token seen again revokes the whole family
    val familyId: String = UUID.randomUUID().toString(),
    // set by the database when the token is issued; none yet on one built in memory
    val createdAt: Instant? = null,
) {
    fun isValid(now: Instant): Boolean = !revoked && expiresAt.isAfter(now)
}
