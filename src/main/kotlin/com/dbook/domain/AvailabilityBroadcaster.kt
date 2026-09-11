package com.dbook.domain

/**
 * Publishes a [Bookable]'s updated availability for real-time delivery to subscribed
 * clients (see the `infrastructure.messaging` package for the WebSocket/Redis Pub/Sub
 * implementation). Always called only after the triggering transaction commits.
 */
interface AvailabilityBroadcaster {
    fun broadcast(
        bookableId: Long,
        availableCapacity: Int,
    )
}
