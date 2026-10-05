package com.dbook.infrastructure.messaging.outbox.sqsoutboxpublisher

import com.dbook.LocalStackSqs
import kotlin.test.Test
import kotlin.test.assertEquals

class SendsThePayloadAndTheHeadersToTheRoutedQueueTest : SqsOutboxPublisherFixture() {
    @Test
    fun `given an expiration event when publishing then the payload is the message and the headers the attributes`() {
        val queue = LocalStackSqs.createQueue()
        val event = event(headers = mapOf("traceparent" to "00-abc-def-01"))

        publisherFor(queue).publish(event)

        val message = LocalStackSqs.receiveWithAttributes(queue, 5).single()
        assertEquals("""{"bookingId":42}""", message.body())
        assertEquals("00-abc-def-01", message.messageAttributes()["traceparent"]?.stringValue())
        assertEquals(event.id.toString(), message.messageAttributes()["eventId"]?.stringValue())
        assertEquals("booking.expiration.requested", message.messageAttributes()["eventType"]?.stringValue())
    }
}
