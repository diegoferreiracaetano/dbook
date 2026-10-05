package com.dbook.application.notification.notificationsettings

import com.dbook.domain.notification.NotificationChannel
import com.dbook.domain.notification.NotificationPreference
import com.dbook.domain.notification.NotificationType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EverythingIsEnabledUntilTheCustomerSwitchesItOffTest : NotificationSettingsFixture() {
    @Test
    fun `given no choice made when reading then every type on every channel is enabled`() {
        val all = getPreferences.execute(7)

        assertEquals(NotificationType.entries.size * NotificationChannel.entries.size, all.size)
        assertTrue(all.all { it.enabled })
    }

    @Test
    fun `given a pair switched off when reading then only that pair is off and the others did not change`() {
        val off = NotificationPreference(NotificationType.FLIGHT_CHANGED, NotificationChannel.PUSH, false)

        val after = updatePreferences.execute(7, listOf(off))

        assertEquals(listOf(off), after.filter { !it.enabled })
        assertTrue(getPreferences.execute(8).all { it.enabled })
    }

    @Test
    fun `given the same pair twice in one request when updating then the last one wins`() {
        val type = NotificationType.BOOKING_EXPIRED
        val changes =
            listOf(
                NotificationPreference(type, NotificationChannel.EMAIL, false),
                NotificationPreference(type, NotificationChannel.EMAIL, true),
            )

        val after = updatePreferences.execute(7, changes)

        assertTrue(after.all { it.enabled })
    }
}
