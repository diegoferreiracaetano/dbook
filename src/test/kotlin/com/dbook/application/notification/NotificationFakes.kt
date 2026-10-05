package com.dbook.application.notification

import com.dbook.domain.notification.DeliveryClaim
import com.dbook.domain.notification.DeviceToken
import com.dbook.domain.notification.DeviceTokenRepository
import com.dbook.domain.notification.Notification
import com.dbook.domain.notification.NotificationChannel
import com.dbook.domain.notification.NotificationDeliveryLog
import com.dbook.domain.notification.NotificationPreference
import com.dbook.domain.notification.NotificationPreferenceRepository
import com.dbook.domain.notification.NotificationRepository
import com.dbook.domain.notification.PushDeliveryException
import com.dbook.domain.notification.PushSender
import java.time.Instant
import java.util.UUID

class InMemoryNotifications : NotificationRepository {
    val all = mutableListOf<Notification>()
    private var lastId = 0L

    override fun save(notification: Notification): Notification? {
        if (notification.eventId != null && all.any { it.eventId == notification.eventId }) return null
        return notification.copy(id = ++lastId).also { all += it }
    }

    override fun findByUser(
        userId: Long,
        beforeId: Long?,
        size: Int,
        unreadOnly: Boolean,
    ): List<Notification> =
        all.filter { it.userId == userId && (beforeId == null || it.id!! < beforeId) && (!unreadOnly || !it.isRead) }
            .sortedByDescending { it.id }
            .take(size)

    override fun countUnread(userId: Long): Long = all.count { it.userId == userId && !it.isRead }.toLong()

    override fun markRead(
        id: Long,
        userId: Long,
        now: Instant,
    ): Boolean {
        val found = all.find { it.id == id && it.userId == userId } ?: return false
        if (!found.isRead) replace(found, found.copy(readAt = now))
        return true
    }

    override fun markAllRead(
        userId: Long,
        now: Instant,
    ): Int {
        val unread = all.filter { it.userId == userId && !it.isRead }
        unread.forEach { replace(it, it.copy(readAt = now)) }
        return unread.size
    }

    private fun replace(
        old: Notification,
        new: Notification,
    ) {
        all[all.indexOf(old)] = new
    }
}

class InMemoryDeliveryLog : NotificationDeliveryLog {
    enum class Status { PENDING, SENT, SKIPPED }

    val statuses = mutableMapOf<Pair<UUID, NotificationChannel>, Status>()
    val errors = mutableListOf<String>()

    override fun claim(
        eventId: UUID,
        channel: NotificationChannel,
    ): DeliveryClaim =
        when (statuses[eventId to channel]) {
            null -> DeliveryClaim.NEW.also { statuses[eventId to channel] = Status.PENDING }
            Status.PENDING -> DeliveryClaim.RETRY
            else -> DeliveryClaim.ALREADY_DONE
        }

    override fun markSent(
        eventId: UUID,
        channel: NotificationChannel,
    ) {
        statuses[eventId to channel] = Status.SENT
    }

    override fun markSkipped(
        eventId: UUID,
        channel: NotificationChannel,
    ) {
        statuses[eventId to channel] = Status.SKIPPED
    }

    override fun markFailed(
        eventId: UUID,
        channel: NotificationChannel,
        error: String,
    ) {
        errors += error
    }
}

class InMemoryPreferences : NotificationPreferenceRepository {
    private val chosen = mutableMapOf<Long, MutableMap<Pair<Any, Any>, NotificationPreference>>()

    override fun findChosen(userId: Long): List<NotificationPreference> = chosen[userId]?.values?.toList().orEmpty()

    override fun save(
        userId: Long,
        preferences: List<NotificationPreference>,
    ) {
        val mine = chosen.getOrPut(userId) { mutableMapOf() }
        preferences.forEach { mine[it.type to it.channel] = it }
    }
}

class InMemoryDevices : DeviceTokenRepository {
    val tokens = mutableListOf<DeviceToken>()

    override fun register(deviceToken: DeviceToken) {
        tokens.removeAll { it.token == deviceToken.token }
        tokens += deviceToken
    }

    override fun remove(
        userId: Long,
        token: String,
    ) {
        tokens.removeAll { it.userId == userId && it.token == token }
    }

    override fun findByUser(userId: Long): List<DeviceToken> = tokens.filter { it.userId == userId }
}

class RecordingPushSender : PushSender {
    val sent = mutableListOf<String>()
    var failing = false

    override fun send(
        devices: List<DeviceToken>,
        title: String,
        body: String,
        data: Map<String, Any?>,
    ) {
        if (failing) throw PushDeliveryException("push service is down")
        sent += "${devices.size}:$title"
    }
}
