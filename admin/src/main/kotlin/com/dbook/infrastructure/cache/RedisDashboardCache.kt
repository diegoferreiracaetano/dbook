package com.dbook.infrastructure.cache

import com.dbook.domain.dashboard.DashboardCache
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * The project's first cache. A dashboard answer is a few aggregate queries over the whole booking history, and the
 * same screen is opened over and over, so it is kept for a minute (`dashboard.cache-ttl-seconds`): a number can be a
 * minute old, and nobody can tell. It never gets in the way: with Redis down the answer is just computed (and the
 * `error` outcome counts it), and a copy that cannot be read is treated as missing.
 *
 * `dbook_cache_total{cache="dashboard", outcome="hit|miss|error"}` says how well it works.
 */
@Component
class RedisDashboardCache(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val meterRegistry: MeterRegistry,
    @Value("\${dashboard.cache-ttl-seconds}") ttlSeconds: Long,
) : DashboardCache {
    private val ttl = Duration.ofSeconds(ttlSeconds)

    override fun <T : Any> remember(
        key: String,
        type: Class<T>,
        load: () -> T,
    ): T {
        val redisKey = "dbook:dashboard:$key"
        val cached = read(redisKey, type)
        if (cached != null) {
            count("hit")
            return cached
        }
        count("miss")
        return load().also { write(redisKey, it) }
    }

    private fun <T : Any> read(
        redisKey: String,
        type: Class<T>,
    ): T? =
        try {
            redisTemplate.opsForValue().get(redisKey)?.let { objectMapper.readValue(it, type) }
        } catch (ex: DataAccessException) {
            unavailable(ex)
        } catch (ex: JsonProcessingException) {
            log.warn("Discarding an unreadable cache entry {}", redisKey, ex)
            null
        }

    private fun write(
        redisKey: String,
        value: Any,
    ) {
        try {
            redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(value), ttl)
        } catch (ex: DataAccessException) {
            unavailable(ex)
        }
    }

    private fun unavailable(ex: DataAccessException): Nothing? {
        log.warn("The dashboard cache is unavailable, answering without it", ex)
        count("error")
        return null
    }

    private fun count(outcome: String) =
        meterRegistry.counter(
            "dbook.cache",
            "cache",
            "dashboard",
            "outcome",
            outcome,
        ).increment()

    private companion object {
        val log: Logger = LoggerFactory.getLogger(RedisDashboardCache::class.java)
    }
}
