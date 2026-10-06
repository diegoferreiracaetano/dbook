package com.dbook.application.notification

import com.dbook.domain.notification.DevicePlatform
import com.dbook.domain.notification.DeviceToken
import com.dbook.domain.notification.DeviceTokenRepository
import com.dbook.domain.notification.NotificationPreference
import com.dbook.domain.notification.NotificationPreferenceRepository
import com.dbook.domain.notification.effectivePreferences
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import java.time.Clock

@Observed(name = "dbook.usecase")
@Service
class GetNotificationPreferencesUseCase(
    private val preferences: NotificationPreferenceRepository,
) {
    /** Every type on every channel, with what is in force: the user's choice, or enabled when they made none. */
    fun execute(userId: Long): List<NotificationPreference> = effectivePreferences(preferences.findChosen(userId))
}

@Observed(name = "dbook.usecase")
@Service
class UpdateNotificationPreferencesUseCase(
    private val preferences: NotificationPreferenceRepository,
) {
    /** Changes only the pairs sent; the rest stay as they were. The last one wins when a pair is repeated. */
    fun execute(
        userId: Long,
        changes: List<NotificationPreference>,
    ): List<NotificationPreference> {
        preferences.save(userId, changes.associateBy { it.type to it.channel }.values.toList())
        return effectivePreferences(preferences.findChosen(userId))
    }
}

@Observed(name = "dbook.usecase")
@Service
class RegisterDeviceUseCase(
    private val deviceTokens: DeviceTokenRepository,
    private val clock: Clock,
) {
    fun execute(
        userId: Long,
        token: String,
        platform: DevicePlatform,
    ) = deviceTokens.register(DeviceToken(userId, token.trim(), platform, clock.instant()))
}

@Observed(name = "dbook.usecase")
@Service
class UnregisterDeviceUseCase(
    private val deviceTokens: DeviceTokenRepository,
) {
    fun execute(
        userId: Long,
        token: String,
    ) = deviceTokens.remove(userId, token)
}
