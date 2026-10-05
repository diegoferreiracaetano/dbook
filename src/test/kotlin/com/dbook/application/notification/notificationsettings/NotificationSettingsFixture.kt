package com.dbook.application.notification.notificationsettings

import com.dbook.application.notification.GetNotificationPreferencesUseCase
import com.dbook.application.notification.InMemoryDevices
import com.dbook.application.notification.InMemoryPreferences
import com.dbook.application.notification.RegisterDeviceUseCase
import com.dbook.application.notification.UnregisterDeviceUseCase
import com.dbook.application.notification.UpdateNotificationPreferencesUseCase
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

abstract class NotificationSettingsFixture {
    protected val now: Instant = Instant.parse("2026-10-04T12:00:00Z")
    protected val preferences = InMemoryPreferences()
    protected val devices = InMemoryDevices()

    protected val getPreferences = GetNotificationPreferencesUseCase(preferences)
    protected val updatePreferences = UpdateNotificationPreferencesUseCase(preferences)
    protected val registerDevice = RegisterDeviceUseCase(devices, Clock.fixed(now, ZoneOffset.UTC))
    protected val unregisterDevice = UnregisterDeviceUseCase(devices)
}
