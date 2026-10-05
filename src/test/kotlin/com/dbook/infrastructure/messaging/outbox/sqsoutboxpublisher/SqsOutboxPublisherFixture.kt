package com.dbook.infrastructure.messaging.outbox.sqsoutboxpublisher

import com.dbook.LocalStackSqs
import com.dbook.infrastructure.messaging.outbox.ClaimedOutboxEvent
import com.dbook.infrastructure.messaging.outbox.OutboxProperties
import com.dbook.infrastructure.messaging.outbox.SqsOutboxPublisher
import software.amazon.awssdk.services.sqs.SqsClient
import java.util.UUID

// A real (LocalStack) SQS: what the publisher does is send to a queue, so that is what is checked.
abstract class SqsOutboxPublisherFixture {
    protected fun publisherFor(
        queueUrl: String,
        client: SqsClient = LocalStackSqs.client,
    ) = SqsOutboxPublisher(client, OutboxProperties(queues = mapOf("booking-expiration" to queueUrl)))

    protected fun event(
        type: String = "booking.expiration.requested",
        payload: String = """{"bookingId":42}""",
        headers: Map<String, String> = emptyMap(),
    ) = ClaimedOutboxEvent(UUID.randomUUID(), type, payload, headers, attempts = 1)
}
