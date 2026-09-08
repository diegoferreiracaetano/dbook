package com.dbook.domain

import java.time.Instant

interface TokenService {
    fun generateAccessToken(user: User): String

    fun generateRefreshToken(user: User): String

    /** Returns the user id encoded in the token, or null if the token is missing/invalid/expired. */
    fun parseUserId(token: String): Long?

    /** Returns the role encoded in the token, or null if the token is missing/invalid/expired. */
    fun parseRole(token: String): Role?

    fun hashToken(token: String): String

    fun refreshTokenExpiresAt(): Instant
}
