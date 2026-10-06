package com.dbook.domain.notification

import java.util.UUID

/** What a claim of an event and channel found. */
enum class DeliveryClaim { NEW, RETRY, ALREADY_DONE }

/**
 * Keeps, for each event and channel, whether it was delivered: that is what makes a queue that delivers a message twice
 * harmless. A channel is claimed before it is tried, and confirmed (or marked skipped) after.
 */
interface NotificationDeliveryLog {
    fun claim(
        eventId: UUID,
        channel: NotificationChannel,
    ): DeliveryClaim

    fun markSent(
        eventId: UUID,
        channel: NotificationChannel,
    )

    fun markSkipped(
        eventId: UUID,
        channel: NotificationChannel,
    )

    fun markFailed(
        eventId: UUID,
        channel: NotificationChannel,
        error: String,
    )
}
