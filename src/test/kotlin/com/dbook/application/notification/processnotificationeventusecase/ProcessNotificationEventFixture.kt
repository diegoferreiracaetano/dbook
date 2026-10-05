package com.dbook.application.notification.processnotificationeventusecase

import com.dbook.application.identity.staff.InMemoryUserRepository
import com.dbook.application.identity.staff.RecordingEmailSender
import com.dbook.application.notification.EmailChannel
import com.dbook.application.notification.InAppChannel
import com.dbook.application.notification.InMemoryDeliveryLog
import com.dbook.application.notification.InMemoryDevices
import com.dbook.application.notification.InMemoryNotifications
import com.dbook.application.notification.InMemoryPreferences
import com.dbook.application.notification.NotificationEvent
import com.dbook.application.notification.ProcessNotificationEventUseCase
import com.dbook.application.notification.PushChannel
import com.dbook.application.notification.RecordingPushSender
import com.dbook.domain.identity.User
import com.dbook.domain.notification.DevicePlatform
import com.dbook.domain.notification.DeviceToken
import com.dbook.domain.notification.NotificationType
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

// Customer 7 has an account (ana@example.com); the clock is 2026-10-04 12:00. Nothing is switched off and no device
// is registered unless a test says so.
abstract class ProcessNotificationEventFixture {
    protected val now: Instant = Instant.parse("2026-10-04T12:00:00Z")
    protected val customerId = 7L
    protected val notifications = InMemoryNotifications()
    protected val deliveryLog = InMemoryDeliveryLog()
    protected val preferences = InMemoryPreferences()
    protected val devices = InMemoryDevices()
    protected val emails = RecordingEmailSender()
    protected val pushes = RecordingPushSender()
    protected val users =
        InMemoryUserRepository(User(customerId, "ana@example.com", "hash", "Ana"))
    protected val meters = SimpleMeterRegistry()

    protected val useCase =
        ProcessNotificationEventUseCase(
            listOf(
                InAppChannel(notifications, Clock.fixed(now, ZoneOffset.UTC)),
                EmailChannel(users, emails),
                PushChannel(devices, pushes),
            ),
            deliveryLog,
            preferences,
            meters,
        )

    protected fun registerDevice() = devices.register(DeviceToken(customerId, "token-1", DevicePlatform.ANDROID, now))

    protected fun confirmedEvent(id: UUID = UUID.randomUUID()) =
        NotificationEvent(
            id,
            NotificationType.BOOKING_CONFIRMED,
            mapOf("bookingId" to 10, "customerId" to customerId, "title" to "GRU-GIG"),
        )

    protected fun counted(
        channel: String,
        outcome: String,
    ) = meters.find("dbook.notification").tags("channel", channel, "outcome", outcome).counter()?.count() ?: 0.0
}
