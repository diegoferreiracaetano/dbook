package com.dbook.presentation.notification.notificationconsumer

import com.dbook.LocalStackSqs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnUnknownEventOrAMessageWithoutItsTypeGoesToTheDeadLetterQueueTest : NotificationConsumerFixture() {
    @Test
    fun `given an unknown type and a message without type when polled repeatedly then both are dead-lettered`() {
        val (token, id) = aCustomer()
        val (queue, deadLetters) =
            LocalStackSqs.createQueueWithDeadLetterQueue(visibilityTimeoutSeconds = 1, maxReceiveCount = 2)
        send(queue, "booking.teleported", id)
        send(queue, null, id)
        val consumer = consumerFor(queue)

        // two deliveries that fail, then the third receive finds both already moved to the dead-letter queue
        repeat(3) {
            consumer.poll()
            waitForRedelivery()
        }

        assertEquals(2, LocalStackSqs.receive(deadLetters, waitSeconds = 3).size)
        assertTrue(LocalStackSqs.receive(queue, waitSeconds = 0).isEmpty())
        assertEquals(0, body(inbox(token))["items"].size())
        assertTrue(meterRegistry.counter("dbook.notification.messages", "outcome", "failed").count() >= 2.0)
    }
}
