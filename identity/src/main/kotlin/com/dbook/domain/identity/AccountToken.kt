package com.dbook.domain.identity

import java.time.Duration
import java.time.Instant

private const val VERIFICATION_HOURS = 48L
private const val RESET_HOURS = 1L

/** What a link mailed to a user is for. The validity is short for the one that can take the account over. */
enum class AccountTokenPurpose(val validity: Duration) {
    EMAIL_VERIFICATION(Duration.ofHours(VERIFICATION_HOURS)),
    PASSWORD_RESET(Duration.ofHours(RESET_HOURS)),
}

/** A single-use link for one user and one purpose. Only the [tokenHash] is kept, never the token that was mailed. */
data class AccountToken(
    val id: Long? = null,
    val userId: Long,
    val purpose: AccountTokenPurpose,
    val tokenHash: String,
    val expiresAt: Instant,
    val createdAt: Instant,
    val usedAt: Instant? = null,
) {
    fun isUsableAt(now: Instant): Boolean = usedAt == null && expiresAt.isAfter(now)

    companion object {
        fun issue(
            userId: Long,
            purpose: AccountTokenPurpose,
            tokenHash: String,
            now: Instant,
        ) = AccountToken(
            userId = userId,
            purpose = purpose,
            tokenHash = tokenHash,
            expiresAt = now.plus(purpose.validity),
            createdAt = now,
        )
    }
}

interface AccountTokenRepository {
    /** Stores the token and closes every older open one of the same user and purpose: one link at a time. */
    fun issue(token: AccountToken): AccountToken

    fun findByTokenHash(tokenHash: String): AccountToken?

    /** Spends a token **atomically**: true for the one caller that found it unspent and still valid. */
    fun consume(
        id: Long,
        now: Instant,
    ): Boolean
}

/** The same error for an unknown, expired, used or wrong-purpose link: the caller learns nothing about its state. */
class InvalidAccountTokenException : RuntimeException("The link is not valid or has expired")

/** The account has not confirmed its e-mail address, and booking and paying ask for it. */
class EmailNotVerifiedException : RuntimeException("Confirm your e-mail address to book and pay")
