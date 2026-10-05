package com.dbook.presentation.notification.notificationconsumer

import com.dbook.LocalStackSqs
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class TheQueueDeliveringTheSameMessageTwiceNotifiesOnceTest : NotificationConsumerFixture() {
    @Test
    fun `given the same event sent twice when polled then one notification, one e-mail`() {
        val (token, id) = aCustomer()
        val queue = LocalStackSqs.createQueue()
        val event = UUID.randomUUID()
        val mailsBefore = emailSender.sent.size
        send(queue, "refund.completed", id, event)
        send(queue, "refund.completed", id, event)

        consumerFor(queue).poll()

        assertEquals(1, body(inbox(token))["items"].size())
        assertEquals(mailsBefore + 1, emailSender.sent.size)
    }
}
