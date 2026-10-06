package com.dbook.domain.notification

import java.time.Instant

interface NotificationRepository {
    /** Keeps the notification unless one for the same event already exists (then it is a duplicate and is dropped). */
    fun save(notification: Notification): Notification?

    /** Newest first, [size] of them after [beforeId] (exclusive). */
    fun findByUser(
        userId: Long,
        beforeId: Long?,
        size: Int,
        unreadOnly: Boolean,
    ): List<Notification>

    fun countUnread(userId: Long): Long

    /** @return false when the notification is not this user's, or does not exist. Reading twice is fine. */
    fun markRead(
        id: Long,
        userId: Long,
        now: Instant,
    ): Boolean

    fun markAllRead(
        userId: Long,
        now: Instant,
    ): Int
}
