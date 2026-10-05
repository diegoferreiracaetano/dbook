package com.dbook.infrastructure.messaging.outbox.sqsoutboxpublisher

import com.dbook.LocalStackSqs
import com.dbook.infrastructure.messaging.outbox.OutboxPublishException
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ATypeWithNoRouteOrAnUnreachableQueueFailsSoTheRelayRetriesTest : SqsOutboxPublisherFixture() {
    @Test
    fun `given an unknown type or an unreachable queue when publishing then it fails, never drops the event`() {
        val queue = LocalStackSqs.createQueue()

        assertFailsWith<OutboxPublishException> { publisherFor(queue).publish(event(type = "nobody.listens.here")) }
        assertFailsWith<OutboxPublishException> {
            publisherFor(
                "http://localhost:1/000000000000/none",
                LocalStackSqs.clientFor(URI.create("http://localhost:1")),
            )
                .publish(event())
        }
    }
}
