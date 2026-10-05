package com.dbook.infrastructure.cache

import com.dbook.domain.catalog.Flight
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.mockito.Mockito
import org.springframework.dao.QueryTimeoutException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TheSearchWorksWhenTheCacheDoesNotTest {
    @Test
    fun `given redis is down when a search is remembered then the database answer is returned and the error counted`() {
        val values = Mockito.mock(ValueOperations::class.java)
        Mockito.`when`(values.get(Mockito.anyString())).thenThrow(QueryTimeoutException("redis is down"))
        val template = Mockito.mock(StringRedisTemplate::class.java)
        Mockito.`when`(template.opsForValue()).thenReturn(values as ValueOperations<String, String>)
        val meters = SimpleMeterRegistry()
        val cache = RedisFlightSearchCache(template, ObjectMapper(), meters, enabled = true, ttlSeconds = 30)
        var loads = 0

        val answer = cache.remember("GRU", "GIG", LocalDate.of(2027, 1, 1)) { loads++.let { emptyList<Flight>() } }

        assertEquals(emptyList(), answer)
        assertEquals(1, loads)
        assertEquals(1.0, meters.counter("dbook.cache", "cache", "flight-search", "outcome", "error").count())
    }
}
