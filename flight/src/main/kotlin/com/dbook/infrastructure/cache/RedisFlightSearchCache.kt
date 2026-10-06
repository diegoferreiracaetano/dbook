package com.dbook.infrastructure.cache

import com.dbook.domain.catalog.AirportChangeListener
import com.dbook.domain.flight.Flight
import com.dbook.domain.flight.FlightSearchCache
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration
import java.time.LocalDate

/**
 * The search results, per route and day, for `flight-search-cache.ttl-seconds`. Invalidating is one `INCR` of an
 * "epoch" that every key carries: the old copies become unreachable at once and expire by themselves, with no
 * `SCAN` over the keys. A reader that loaded from the database just before a change stores under the old epoch, so
 * its stale answer is never read again. With Redis down it never gets in the way: the search just goes to the
 * database (outcome `error`). The metric `dbook_cache_total{cache="flight-search"}` (outcome hit, miss or error)
 * shows how it does.
 */
@Component
class RedisFlightSearchCache(
    private val redisTemplate: StringRedisTemplate,
    objectMapper: ObjectMapper,
    private val meterRegistry: MeterRegistry,
    @Value("\${flight-search-cache.enabled:true}") private val enabled: Boolean,
    @Value("\${flight-search-cache.ttl-seconds:30}") ttlSeconds: Long,
) : FlightSearchCache, AirportChangeListener {
    private val ttl = Duration.ofSeconds(ttlSeconds)
    private val snapshot = FlightSnapshot(objectMapper)

    override fun remember(
        originIataCode: String,
        destinationIataCode: String,
        date: LocalDate,
        load: () -> List<Flight>,
    ): List<Flight> {
        // the epoch is read once, before the database is: whatever is loaded is filed under the epoch it was
        // asked in, so a change that lands meanwhile makes this copy unreachable instead of stale-but-current
        val key = if (enabled) guarded { keyOf(originIataCode, destinationIataCode, date) } else null
        val cached = key?.let { k -> guarded { redisTemplate.opsForValue().get(k)?.let(snapshot::read) } }
        if (key != null) count(if (cached != null) "hit" else "miss")
        return cached ?: load().also { flights -> key?.let { store(it, flights) } }
    }

    private fun store(
        key: String,
        flights: List<Flight>,
    ) {
        guarded { redisTemplate.opsForValue().set(key, snapshot.write(flights), ttl) }
    }

    // the results show each flight's airports: when one changes, the copies are stale
    override fun airportChanged() = invalidateAll()

    override fun invalidateAll() {
        if (!enabled) return
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() = bumpEpoch()
                },
            )
        } else {
            bumpEpoch()
        }
    }

    private fun bumpEpoch() {
        guarded { redisTemplate.opsForValue().increment(EPOCH_KEY) }
    }

    private fun keyOf(
        origin: String,
        destination: String,
        date: LocalDate,
    ): String = "dbook:flight-search:${redisTemplate.opsForValue().get(EPOCH_KEY) ?: "0"}:$origin:$destination:$date"

    // a failure of the cache (Redis down, a copy that cannot be read) is a miss, never an error for the customer
    private fun <T> guarded(block: () -> T?): T? =
        try {
            block()
        } catch (ex: DataAccessException) {
            failed(ex)
        } catch (ex: JsonProcessingException) {
            failed(ex)
        }

    private fun <T> failed(ex: Exception): T? {
        count("error")
        log.warn("The flight search cache failed, going to the database", ex)
        return null
    }

    private fun count(outcome: String) {
        meterRegistry.counter("dbook.cache", "cache", "flight-search", "outcome", outcome).increment()
    }

    private companion object {
        const val EPOCH_KEY = "dbook:flight-search:epoch"
        val log: Logger = LoggerFactory.getLogger(RedisFlightSearchCache::class.java)
    }
}
