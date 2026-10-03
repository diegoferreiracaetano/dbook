package com.dbook.presentation.booking.bookingexpirationconsumer

import com.dbook.LocalStackSqs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SendsAPoisonMessageToTheDeadLetterQueueTest : BookingExpirationConsumerFixture() {
    @Test
    fun `given a message that always fails when polled repeatedly then it ends up in the dead letter queue`() {
        val (queueUrl, deadLetterUrl) =
            LocalStackSqs.createQueueWithDeadLetterQueue(visibilityTimeoutSeconds = 1, maxReceiveCount = 2)
        LocalStackSqs.send(queueUrl, "this is not json")
        val consumer = consumerFor(queueUrl)

        // 2 deliveries that fail, then the third receive finds it already moved to the DLQ
        repeat(3) {
            withTransactionSynchronization { consumer.poll() }
            waitForRedelivery()
        }

        val deadLettered = LocalStackSqs.receive(deadLetterUrl, waitSeconds = 3)
        assertEquals(listOf("this is not json"), deadLettered.map { it.body() })
        assertTrue(LocalStackSqs.receive(queueUrl, waitSeconds = 0).isEmpty())
    }
}
