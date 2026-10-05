package com.dbook.application.notification

import com.dbook.domain.common.PageQuery
import com.dbook.domain.notification.Notification
import com.dbook.domain.notification.NotificationRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import java.time.Clock

class NotificationNotFoundException(id: Long) : RuntimeException("Notification not found: $id")

/** A page of the inbox, newest first: [next] is the cursor of the next page (the id to continue before), or null. */
data class NotificationPage(
    val items: List<Notification>,
    val next: Long?,
)

@Observed(name = "dbook.usecase")
@Service
class ListNotificationsUseCase(
    private val notifications: NotificationRepository,
) {
    fun execute(
        userId: Long,
        before: Long?,
        size: Int,
        unreadOnly: Boolean,
    ): NotificationPage {
        require(size in 1..PageQuery.MAX_SIZE) { "size must be between 1 and ${PageQuery.MAX_SIZE}" }
        // one more than asked, only to know whether there is a next page
        val found = notifications.findByUser(userId, before, size + 1, unreadOnly)
        val items = found.take(size)
        return NotificationPage(items, if (found.size > size) items.last().id else null)
    }
}

@Observed(name = "dbook.usecase")
@Service
class CountUnreadNotificationsUseCase(
    private val notifications: NotificationRepository,
) {
    fun execute(userId: Long): Long = notifications.countUnread(userId)
}

@Observed(name = "dbook.usecase")
@Service
class MarkNotificationReadUseCase(
    private val notifications: NotificationRepository,
    private val clock: Clock,
) {
    // someone else's notification looks exactly like one that does not exist
    fun execute(
        userId: Long,
        id: Long,
    ) {
        if (!notifications.markRead(id, userId, clock.instant())) {
            throw NotificationNotFoundException(id)
        }
    }
}

@Observed(name = "dbook.usecase")
@Service
class MarkAllNotificationsReadUseCase(
    private val notifications: NotificationRepository,
    private val clock: Clock,
) {
    fun execute(userId: Long): Int = notifications.markAllRead(userId, clock.instant())
}
