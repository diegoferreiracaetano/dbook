package com.dbook.infrastructure.security

import kotlin.test.Test
import kotlin.test.assertNull

class ClearingAKeyFreesItTest : RedisLoginAttemptLimiterFixture() {
    @Test
    fun `given a locked key when it is cleared then it may try again`() {
        val key = uniqueKey()
        repeat(3) { limiter.recordFailure(key) }

        limiter.clear(key)

        assertNull(limiter.retryAfterSeconds(key, maxFailures = 3))
    }
}
