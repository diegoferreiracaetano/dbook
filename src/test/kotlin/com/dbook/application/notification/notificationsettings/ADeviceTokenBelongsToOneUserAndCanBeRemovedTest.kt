package com.dbook.application.notification.notificationsettings

import com.dbook.domain.notification.DevicePlatform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ADeviceTokenBelongsToOneUserAndCanBeRemovedTest : NotificationSettingsFixture() {
    @Test
    fun `given a token registered by another user when this user registers it then it moves to this user`() {
        registerDevice.execute(7, " token-1 ", DevicePlatform.ANDROID)

        registerDevice.execute(8, "token-1", DevicePlatform.IOS)

        assertTrue(devices.findByUser(7).isEmpty())
        assertEquals(listOf("token-1"), devices.findByUser(8).map { it.token })
    }

    @Test
    fun `given someone else's token when it is unregistered then it stays, and the owner can remove it`() {
        registerDevice.execute(7, "token-1", DevicePlatform.WEB)

        unregisterDevice.execute(8, "token-1")
        assertEquals(1, devices.findByUser(7).size)

        unregisterDevice.execute(7, "token-1")
        assertTrue(devices.findByUser(7).isEmpty())
    }

    @Test
    fun `given a blank token when registering then it is refused`() {
        assertFailsWith<IllegalArgumentException> { registerDevice.execute(7, "  ", DevicePlatform.WEB) }
    }
}
