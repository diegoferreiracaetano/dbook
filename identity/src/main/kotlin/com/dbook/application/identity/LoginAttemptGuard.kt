package com.dbook.application.identity

import com.dbook.domain.identity.LoginAttemptLimiter
import org.springframework.stereotype.Service

data class LoginAttemptsPolicy(
    val maxFailuresPerEmail: Int,
    val maxFailuresPerIp: Int,
)

// Two keys: the email catches someone attacking one account, the IP catches someone trying many accounts.
@Service
class LoginAttemptGuard(
    private val limiter: LoginAttemptLimiter,
    private val policy: LoginAttemptsPolicy,
) {
    /** Seconds the caller must wait, or null when it may try. */
    fun lockedFor(
        email: String,
        ip: String,
    ): Long? =
        limiter.retryAfterSeconds(emailKey(email), policy.maxFailuresPerEmail)
            ?: limiter.retryAfterSeconds(ipKey(ip), policy.maxFailuresPerIp)

    fun recordFailure(
        email: String,
        ip: String,
    ) {
        limiter.recordFailure(emailKey(email))
        limiter.recordFailure(ipKey(ip))
    }

    // only the email is cleared: the address may be shared by people who are still failing
    fun clear(email: String) = limiter.clear(emailKey(email))

    private fun emailKey(email: String) = "email:${email.trim().lowercase()}"

    private fun ipKey(ip: String) = "ip:$ip"
}
