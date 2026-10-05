package com.dbook.presentation.notification

import com.dbook.domain.notification.NotificationType
import kotlin.test.Test
import kotlin.test.assertEquals

class APreferenceSwitchedOffStopsThatChannelForThatTypeTest : NotificationFixture() {
    @Test
    fun `given no choice when reading preferences then every pair is enabled`() {
        val (token, _) = aCustomer()

        val all = body(preferences(token))

        assertEquals(18, all.size())
        assertEquals(true, all.all { it["enabled"].asBoolean() })
    }

    @Test
    fun `given the e-mail off for confirmations when delivered then no e-mail goes out but the inbox is written`() {
        val (token, id) = aCustomer()
        val changed =
            updatePreferences(
                token,
                listOf(mapOf("type" to "BOOKING_CONFIRMED", "channel" to "EMAIL", "enabled" to false)),
            )
        val mailsBefore = emailSender.sent.size

        deliver(id, NotificationType.BOOKING_CONFIRMED)
        deliver(id, NotificationType.BOOKING_EXPIRED)

        assertEquals(200, changed.response.status)
        assertEquals(1, body(changed).count { !it["enabled"].asBoolean() })
        assertEquals(mailsBefore + 1, emailSender.sent.size)
        assertEquals(2, body(inbox(token))["items"].size())
    }

    @Test
    fun `given an unknown type when updating preferences then it is a 400`() {
        val (token, _) = aCustomer()

        val result = updatePreferences(token, listOf(mapOf("type" to "NOPE", "channel" to "EMAIL", "enabled" to false)))

        assertEquals(400, result.response.status)
    }
}
