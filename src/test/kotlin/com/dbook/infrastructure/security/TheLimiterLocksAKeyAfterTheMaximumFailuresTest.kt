package com.dbook.infrastructure.security

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TheLimiterLocksAKeyAfterTheMaximumFailuresTest : RedisLoginAttemptLimiterFixture() {
    @Test
    fun `given failures up to the maximum when counting then it is locked, with a wait, only at the maximum`() {
        val key = uniqueKey()
        repeat(2) { limiter.recordFailure(key) }
        assertNull(limiter.retryAfterSeconds(key, maxFailures = 3))

        limiter.recordFailure(key)

        val wait = assertNotNull(limiter.retryAfterSeconds(key, maxFailures = 3))
        // the counter always carries an expiry (15 minutes), so a lock can never be forever
        assertTrue(wait in 1..15 * 60)
    }
}
