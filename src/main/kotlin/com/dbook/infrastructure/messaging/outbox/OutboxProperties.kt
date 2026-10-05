package com.dbook.infrastructure.messaging.outbox

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * [queues] maps a routing key to a queue url. An event type routes by everything before its last dot, with the dots
 * turned into dashes: `booking.expiration.requested` goes to `booking-expiration`.
 */
@ConfigurationProperties(prefix = "outbox")
data class OutboxProperties(
    val queues: Map<String, String> = emptyMap(),
    val relay: Relay = Relay(),
) {
    data class Relay(
        val batchSize: Int = DEFAULT_BATCH,
        // how long a relay holds an event it claimed before another may take it (it crashed, or is just slow)
        val leaseSeconds: Long = DEFAULT_LEASE_SECONDS,
        val retentionDays: Long = DEFAULT_RETENTION_DAYS,
    )

    private companion object {
        const val DEFAULT_BATCH = 50
        const val DEFAULT_LEASE_SECONDS = 60L
        const val DEFAULT_RETENTION_DAYS = 7L
    }
}
