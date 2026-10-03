package com.dbook.presentation.booking.bookingexpirationconsumer

import com.dbook.LocalStackSqs
import kotlin.test.Test
import kotlin.test.assertEquals

class CountsAFailedMessageTest : BookingExpirationConsumerFixture() {
    @Test
    fun `given a message that cannot be processed when polled then it is counted as failed`() {
        val queueUrl = LocalStackSqs.createQueue()
        LocalStackSqs.send(queueUrl, "this is not json")

        withTransactionSynchronization { consumerFor(queueUrl).poll() }

        assertEquals(1.0, counted("dbook.booking.expiration", "failed"))
    }
}
