package com.dbook.application.notification

import com.dbook.domain.notification.NotificationType
import java.util.UUID

/** An outbox event as the notifications side receives it: which one it is, what it is, and what it says. */
data class NotificationEvent(
    val eventId: UUID,
    val type: NotificationType,
    val data: Map<String, Any?>,
) {
    /** The customer the event is about: every notification event carries one. */
    val customerId: Long
        get() = (data["customerId"] as? Number)?.toLong() ?: error("The event $eventId has no customerId")
}
