package com.dbook.presentation.notification.notificationconsumer

import com.dbook.LocalStackSqs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AMessageBecomesANotificationAndIsDeletedTest : NotificationConsumerFixture() {
    @Test
    fun `given a booking confirmed message when polled then the notification exists and the queue is empty`() {
        val (token, id) = aCustomer()
        val queue = LocalStackSqs.createQueue()
        send(queue, "booking.confirmed", id)

        consumerFor(queue).poll()

        val items = body(inbox(token))["items"]
        assertEquals(listOf("BOOKING_CONFIRMED"), items.map { it["type"].asText() })
        assertEquals(1, items[0]["data"]["bookingId"].asInt())
        waitForRedelivery()
        assertTrue(LocalStackSqs.receive(queue, waitSeconds = 0).isEmpty())
    }
}
