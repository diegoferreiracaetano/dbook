package com.dbook.domain.messaging

/**
 * Where a use case puts an event, **inside its transaction**: the event commits with the change or does not exist
 * at all. It fails outside a transaction, on purpose: an event written alone could be lost or sent for a change that
 * never happened, which is the whole problem the outbox exists to prevent.
 */
interface OutboxWriter {
    fun add(event: OutboxEvent)
}
