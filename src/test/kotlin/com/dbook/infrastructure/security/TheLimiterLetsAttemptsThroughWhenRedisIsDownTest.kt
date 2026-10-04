package com.dbook.infrastructure.security

import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.StringRedisTemplate
import kotlin.test.Test
import kotlin.test.assertNull

// A supporting store being down must not become a login outage: the limiter fails open (and logs).
class TheLimiterLetsAttemptsThroughWhenRedisIsDownTest {
    @Test
    fun `given Redis failing when checking and recording then nothing throws and the attempt is allowed`() {
        val redis = mock(StringRedisTemplate::class.java)
        `when`(redis.opsForValue()).thenThrow(RedisConnectionFailureException("down"))
        val limiter = RedisLoginAttemptLimiter(redis, windowMinutes = 15)

        assertNull(limiter.retryAfterSeconds("email:a@b.com", maxFailures = 3))
        limiter.recordFailure("email:a@b.com")
        limiter.clear("email:a@b.com")
    }
}
