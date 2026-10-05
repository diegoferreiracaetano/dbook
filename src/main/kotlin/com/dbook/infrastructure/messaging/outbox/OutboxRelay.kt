package com.dbook.infrastructure.messaging.outbox

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import kotlin.math.min

/**
 * Takes the events that are due out of the outbox and delivers them, at least once. An event that fails is retried
 * with an exponential backoff (5 s, 10 s, 20 s... up to 15 minutes); one that was delivered but whose mark was lost (a
 * crash) is delivered again when its lease runs out, so **a consumer must tolerate a duplicate** (the expiration does:
 * a booking already paid or cancelled is simply ignored). Order between events is **not** guaranteed.
 */
@Component
class OutboxRelay(
    private val store: OutboxStore,
    private val publisher: OutboxPublisher,
    private val properties: OutboxProperties,
    private val meterRegistry: MeterRegistry,
    private val clock: Clock,
) {
    /** Delivers everything that is due now, a batch at a time; returns how many went out. */
    fun relayDueEvents(): Int {
        var delivered = 0
        do {
            val batch =
                store.claim(
                    clock.instant(),
                    properties.relay.batchSize,
                    clock.instant().plusSeconds(properties.relay.leaseSeconds),
                )
            batch.forEach { if (deliver(it)) delivered++ }
        } while (batch.size == properties.relay.batchSize)
        return delivered
    }

    /** Deletes what was published more than the retention ago. */
    fun cleanUp(): Int =
        store.deletePublishedBefore(
            clock.instant().minus(Duration.ofDays(properties.relay.retentionDays)),
        )

    private fun deliver(event: ClaimedOutboxEvent): Boolean =
        try {
            publisher.publish(event)
            store.markPublished(event.id, clock.instant())
            meterRegistry.counter("dbook.outbox.published", "type", event.type).increment()
            true
        } catch (ex: OutboxPublishException) {
            log.warn("Could not deliver outbox event {} ({}), attempt {}", event.id, event.type, event.attempts, ex)
            store.markFailed(event.id, clock.instant().plus(backoff(event.attempts)), ex.message ?: "delivery failed")
            meterRegistry.counter("dbook.outbox.failures", "type", event.type).increment()
            false
        }

    private fun backoff(attempts: Int): Duration =
        Duration.ofSeconds(min(BASE_SECONDS shl (attempts - 1).coerceIn(0, MAX_SHIFT), MAX_SECONDS))

    private companion object {
        const val BASE_SECONDS = 5L
        const val MAX_SECONDS = 15 * 60L
        const val MAX_SHIFT = 10
        val log: Logger = LoggerFactory.getLogger(OutboxRelay::class.java)
    }
}
