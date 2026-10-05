package com.dbook.presentation.notification

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class TheSameEventTwiceIsOneNotificationAndOneEmailTest : NotificationFixture() {
    @Test
    fun `given an event delivered twice when processed twice then one notification and one e-mail`() {
        val (token, id) = aCustomer()
        val event = UUID.randomUUID()
        val mailsBefore = emailSender.sent.size

        deliver(id, eventId = event)
        deliver(id, eventId = event)

        assertEquals(1, body(inbox(token))["items"].size())
        assertEquals(mailsBefore + 1, emailSender.sent.size)
        assertEquals(3, count("SELECT count(*) FROM notification_delivery WHERE event_id = ?::uuid", event.toString()))
    }
}
