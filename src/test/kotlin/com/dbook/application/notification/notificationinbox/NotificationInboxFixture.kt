package com.dbook.application.notification.notificationinbox

import com.dbook.application.notification.CountUnreadNotificationsUseCase
import com.dbook.application.notification.InMemoryNotifications
import com.dbook.application.notification.ListNotificationsUseCase
import com.dbook.application.notification.MarkAllNotificationsReadUseCase
import com.dbook.application.notification.MarkNotificationReadUseCase
import com.dbook.domain.notification.Notification
import com.dbook.domain.notification.NotificationType
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

// Customer 7 has five notifications (ids 1..5), customer 8 has one (id 6). The clock is 2026-10-04 12:00.
abstract class NotificationInboxFixture {
    protected val now: Instant = Instant.parse("2026-10-04T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    protected val notifications = InMemoryNotifications()

    init {
        repeat(5) { add(7, "mine $it") }
        add(8, "someone else's")
    }

    protected val list = ListNotificationsUseCase(notifications)
    protected val countUnread = CountUnreadNotificationsUseCase(notifications)
    protected val markRead = MarkNotificationReadUseCase(notifications, clock)
    protected val markAllRead = MarkAllNotificationsReadUseCase(notifications, clock)

    private fun add(
        userId: Long,
        title: String,
    ) = notifications.save(
        Notification(
            userId = userId,
            type = NotificationType.BOOKING_CONFIRMED,
            title = title,
            body = "body",
            data = emptyMap(),
            eventId = null,
            readAt = null,
            createdAt = now,
        ),
    )
}
