package com.dbook.domain.identity

interface LoginAttemptLimiter {
    /** Seconds until [key] may try again, or null while it has fewer than [maxFailures] failures. */
    fun retryAfterSeconds(
        key: String,
        maxFailures: Int,
    ): Long?

    fun recordFailure(key: String)

    fun clear(key: String)
}
