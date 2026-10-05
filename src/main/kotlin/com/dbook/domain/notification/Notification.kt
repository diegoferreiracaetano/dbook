package com.dbook.domain.notification

import java.time.Instant
import java.util.UUID

/** One notification in the customer's inbox. [eventId] ties it to the event that caused it (null for none). */
data class Notification(
    val id: Long? = null,
    val userId: Long,
    val type: NotificationType,
    val title: String,
    val body: String,
    val data: Map<String, Any?>,
    val eventId: UUID?,
    val readAt: Instant?,
    val createdAt: Instant,
) {
    init {
        require(title.isNotBlank() && body.isNotBlank()) { "a notification needs a title and a body" }
    }

    val isRead: Boolean get() = readAt != null
}
