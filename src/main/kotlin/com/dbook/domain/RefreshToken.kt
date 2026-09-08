package com.dbook.domain

import java.time.Instant

class RefreshToken(
    val id: Long? = null,
    val userId: Long,
    val tokenHash: String,
    val expiresAt: Instant,
    val revoked: Boolean = false,
) {
    fun isValid(now: Instant): Boolean = !revoked && expiresAt.isAfter(now)
}
