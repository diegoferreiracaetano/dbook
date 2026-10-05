package com.dbook.infrastructure.persistence.notification

import com.dbook.domain.notification.DeliveryClaim
import com.dbook.domain.notification.NotificationChannel
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class AClaimIsNewThenARetryUntilItIsDoneTest : NotificationDeliveryLogFixture() {
    @Test
    fun `given a claim never confirmed when claimed again then it is a RETRY with one more attempt`() {
        val event = UUID.randomUUID()

        assertEquals(DeliveryClaim.NEW, deliveryLog.claim(event, NotificationChannel.EMAIL))
        deliveryLog.markFailed(event, NotificationChannel.EMAIL, "smtp down")

        assertEquals(DeliveryClaim.RETRY, deliveryLog.claim(event, NotificationChannel.EMAIL))
        assertEquals(2, attemptsOf(event, "EMAIL"))
        assertEquals("PENDING", statusOf(event, "EMAIL"))
    }

    @Test
    fun `given a channel sent or skipped when claimed again then it is ALREADY_DONE and nothing changes`() {
        val event = UUID.randomUUID()
        deliveryLog.claim(event, NotificationChannel.EMAIL)
        deliveryLog.markSent(event, NotificationChannel.EMAIL)
        deliveryLog.claim(event, NotificationChannel.PUSH)
        deliveryLog.markSkipped(event, NotificationChannel.PUSH)

        assertEquals(DeliveryClaim.ALREADY_DONE, deliveryLog.claim(event, NotificationChannel.EMAIL))
        assertEquals(DeliveryClaim.ALREADY_DONE, deliveryLog.claim(event, NotificationChannel.PUSH))
        assertEquals(1, attemptsOf(event, "EMAIL"))
        assertEquals("SENT", statusOf(event, "EMAIL"))
        assertEquals("SKIPPED", statusOf(event, "PUSH"))
    }

    @Test
    fun `given the same event on another channel when claimed then it is NEW, the channels are independent`() {
        val event = UUID.randomUUID()
        deliveryLog.claim(event, NotificationChannel.EMAIL)
        deliveryLog.markSent(event, NotificationChannel.EMAIL)

        assertEquals(DeliveryClaim.NEW, deliveryLog.claim(event, NotificationChannel.IN_APP))
    }
}
