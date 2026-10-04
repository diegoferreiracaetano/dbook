package com.dbook.infrastructure.security

import com.dbook.domain.identity.LoginAttemptLimiter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

// Redis, not memory: the counters are shared by every instance and expire by themselves.
// It fails open: if Redis is down, a login still works instead of the outage taking logins down too.
@Component
class RedisLoginAttemptLimiter(
    private val redisTemplate: StringRedisTemplate,
    @Value("\${login-attempts.window-minutes}") windowMinutes: Long,
) : LoginAttemptLimiter {
    private val window = Duration.ofMinutes(windowMinutes)

    override fun retryAfterSeconds(
        key: String,
        maxFailures: Int,
    ): Long? =
        failOpen(null) {
            val failures = redisTemplate.opsForValue().get(redisKey(key))?.toLongOrNull() ?: 0
            if (failures < maxFailures) {
                null
            } else {
                redisTemplate.getExpire(redisKey(key), TimeUnit.SECONDS).coerceAtLeast(1)
            }
        }

    override fun recordFailure(key: String) {
        failOpen(Unit) {
            val failures = redisTemplate.opsForValue().increment(redisKey(key))
            // also re-armed when a crash left a counter without expiry, which would lock the key forever
            if (failures == 1L || redisTemplate.getExpire(redisKey(key)) < 0) {
                redisTemplate.expire(redisKey(key), window)
            }
        }
    }

    override fun clear(key: String) {
        failOpen(Unit) { redisTemplate.delete(redisKey(key)) }
    }

    private fun redisKey(key: String) = "dbook:login-failures:$key"

    private fun <T> failOpen(
        fallback: T,
        block: () -> T,
    ): T =
        try {
            block()
        } catch (ex: DataAccessException) {
            log.warn("Login attempt limiter unavailable, letting the attempt through", ex)
            fallback
        }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(RedisLoginAttemptLimiter::class.java)
    }
}
