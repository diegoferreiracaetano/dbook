package com.dbook.infrastructure.cache

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

data class Sample(val name: String, val total: Long)

// A cache over a mocked Redis, a real JSON mapper and a real meter registry.
abstract class RedisDashboardCacheFixture {
    protected val redis: StringRedisTemplate = mock(StringRedisTemplate::class.java)

    @Suppress("UNCHECKED_CAST")
    protected val values: ValueOperations<String, String> =
        mock(
            ValueOperations::class.java,
        ) as ValueOperations<String, String>
    protected val meters = SimpleMeterRegistry()
    protected val cache =
        RedisDashboardCache(redis, Jackson2ObjectMapperBuilder.json().build<ObjectMapper>(), meters, 60)
    protected var loads = 0

    init {
        `when`(redis.opsForValue()).thenReturn(values)
    }

    protected fun load(): Sample {
        loads++
        return Sample("fresh", 7)
    }

    protected fun counted(outcome: String): Double =
        meters.find("dbook.cache").tag("outcome", outcome).counter()?.count() ?: 0.0
}
