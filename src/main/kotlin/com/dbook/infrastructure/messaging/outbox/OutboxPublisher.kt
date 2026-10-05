package com.dbook.infrastructure.messaging.outbox

/** Takes an event to where it goes. */
interface OutboxPublisher {
    /** @throws OutboxPublishException if it could not be delivered: the relay will try again later. */
    fun publish(event: ClaimedOutboxEvent)
}

class OutboxPublishException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
