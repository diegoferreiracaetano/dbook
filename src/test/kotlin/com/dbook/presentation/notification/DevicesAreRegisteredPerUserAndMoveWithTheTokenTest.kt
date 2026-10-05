package com.dbook.presentation.notification

import kotlin.test.Test
import kotlin.test.assertEquals

class DevicesAreRegisteredPerUserAndMoveWithTheTokenTest : NotificationFixture() {
    @Test
    fun `given a token when registered twice then there is one row, and another user signing in moves it`() {
        val (token, id) = aCustomer()
        val (otherToken, otherId) = aCustomer()
        val device = "device-${(1..999_999).random()}"

        assertEquals(204, registerDevice(token, device).response.status)
        registerDevice(token, device)
        assertEquals(1, count("SELECT count(*) FROM device_token WHERE token = ?", device))

        registerDevice(otherToken, device)

        assertEquals(0, count("SELECT count(*) FROM device_token WHERE token = ? AND user_id = ?", device, id))
        assertEquals(1, count("SELECT count(*) FROM device_token WHERE token = ? AND user_id = ?", device, otherId))
    }

    @Test
    fun `given someone else's token when unregistering then it stays, and the owner can remove it`() {
        val (token, _) = aCustomer()
        val (otherToken, _) = aCustomer()
        val device = "device-${(1..999_999).random()}"
        registerDevice(token, device)

        unregisterDevice(otherToken, device)
        assertEquals(1, count("SELECT count(*) FROM device_token WHERE token = ?", device))

        assertEquals(204, unregisterDevice(token, device).response.status)
        assertEquals(0, count("SELECT count(*) FROM device_token WHERE token = ?", device))
    }
}
