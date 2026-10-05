package com.dbook.infrastructure.web

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration

/**
 * Counts events per key in a fixed window, in Redis (shared by every instance, expiring by itself). The window is a
 * minute: the key carries the minute, so the counter is born and dies with it. **Fails open**: when Redis is down the
 * request is let through, because an outage of the limiter must not become an outage of the API.
 */
@Component
class FixedWindowCounter(
    private val redisTemplate: StringRedisTemplate,
    private val clock: Clock,
) {
    /** How many events [key] has had this minute, this one included; null when Redis cannot be asked. */
    fun hit(key: String): Long? =
        try {
            val redisKey = "dbook:rate:$key:${clock.instant().epochSecond / WINDOW.seconds}"
            val count = redisTemplate.opsForValue().increment(redisKey)
            if (count == 1L) {
                redisTemplate.expire(redisKey, WINDOW.plusSeconds(GRACE_SECONDS))
            }
            count
        } catch (ex: DataAccessException) {
            log.warn("Rate limit counter unavailable, letting the request through", ex)
            null
        }

    fun secondsLeftInWindow(): Long = WINDOW.seconds - clock.instant().epochSecond % WINDOW.seconds

    private companion object {
        val WINDOW: Duration = Duration.ofMinutes(1)
        const val GRACE_SECONDS = 5L
        val log: Logger = LoggerFactory.getLogger(FixedWindowCounter::class.java)
    }
}
