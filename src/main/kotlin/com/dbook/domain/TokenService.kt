package com.dbook.domain

import java.time.Instant

/** Issues and parses JWT access/refresh tokens — the domain never handles JWT internals directly. */
interface TokenService {
    /** Short-lived token carrying the user's id and role, sent on every authenticated request. */
    fun generateAccessToken(user: User): String

    /** Long-lived token used only to obtain a new access token; rotated on every use (see [RefreshTokenRepository]). */
    fun generateRefreshToken(user: User): String

    /** Returns the user id encoded in the token, or null if the token is missing/invalid/expired. */
    fun parseUserId(token: String): Long?

    /** Returns the role encoded in the token, or null if the token is missing/invalid/expired. */
    fun parseRole(token: String): Role?

    fun hashToken(token: String): String

    fun refreshTokenExpiresAt(): Instant
}
