package com.dbook.infrastructure.cache

import org.mockito.Mockito.`when`
import org.springframework.data.redis.RedisConnectionFailureException
import kotlin.test.Test
import kotlin.test.assertEquals

class WithRedisDownTheAnswerIsStillComputedTest : RedisDashboardCacheFixture() {
    @Test
    fun `given Redis down when asking then the answer is computed, never an error, and the failure is counted`() {
        `when`(values.get("dbook:dashboard:k")).thenThrow(RedisConnectionFailureException("down"))
        `when`(values.set("dbook:dashboard:k", """{"name":"fresh","total":7}""", java.time.Duration.ofSeconds(60)))
            .thenThrow(RedisConnectionFailureException("down"))

        val answer = cache.remember("k", Sample::class.java, ::load)

        assertEquals(Sample("fresh", 7), answer)
        assertEquals(1, loads)
        assertEquals(2.0, counted("error"))
    }
}
