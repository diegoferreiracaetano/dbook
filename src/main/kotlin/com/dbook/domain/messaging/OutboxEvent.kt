package com.dbook.domain.messaging

import java.time.Instant

/**
 * Something that happened and that another part of the system (or another system) has to hear about. It is added to the
 * outbox in the same transaction as the change itself and delivered later: [availableAt] is the earliest moment it may
 * go out (a booking's expiration is due 15 minutes after the booking, so it carries that moment, not a delay).
 * The payload is plain data; how it is serialized is not the domain's business.
 */
data class OutboxEvent(
    val aggregateType: String,
    val aggregateId: String,
    val type: String,
    val payload: Map<String, Any?>,
    val availableAt: Instant,
) {
    init {
        require(type.isNotBlank() && aggregateType.isNotBlank() && aggregateId.isNotBlank()) {
            "an outbox event needs a type and an aggregate"
        }
    }
}
