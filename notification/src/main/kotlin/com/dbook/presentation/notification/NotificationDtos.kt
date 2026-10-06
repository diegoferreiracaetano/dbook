package com.dbook.presentation.notification

import com.dbook.domain.notification.DevicePlatform
import com.dbook.domain.notification.Notification
import com.dbook.domain.notification.NotificationChannel
import com.dbook.domain.notification.NotificationPreference
import com.dbook.domain.notification.NotificationType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class NotificationResponse(
    val id: Long?,
    val type: NotificationType,
    val title: String,
    val body: String,
    val data: Map<String, Any?>,
    val read: Boolean,
    val createdAt: Instant,
) {
    companion object {
        fun from(notification: Notification) =
            NotificationResponse(
                notification.id,
                notification.type,
                notification.title,
                notification.body,
                notification.data,
                notification.isRead,
                notification.createdAt,
            )
    }
}

data class UnreadCountResponse(val count: Long)

data class MarkedReadResponse(val updated: Int)

data class PreferenceDto(
    @get:Schema(example = "BOOKING_CONFIRMED")
    val type: NotificationType,
    @get:Schema(example = "EMAIL")
    val channel: NotificationChannel,
    @get:Schema(example = "false")
    val enabled: Boolean,
) {
    fun toDomain() = NotificationPreference(type, channel, enabled)

    companion object {
        fun from(preference: NotificationPreference) =
            PreferenceDto(preference.type, preference.channel, preference.enabled)
    }
}

data class RegisterDeviceRequest(
    @get:Schema(example = "fcm-token-abc123", description = "the push token the device got from its platform")
    val token: String,
    @get:Schema(example = "ANDROID")
    val platform: DevicePlatform,
)
