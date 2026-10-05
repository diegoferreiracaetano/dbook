package com.dbook.presentation.booking.bookingexpirationconsumer

import com.dbook.LocalStackSqs
import com.dbook.TracingTestSupport
import com.dbook.infrastructure.messaging.outbox.ClaimedOutboxEvent
import com.dbook.infrastructure.messaging.outbox.OutboxProperties
import com.dbook.infrastructure.messaging.outbox.SqsOutboxPublisher
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

// The expiration runs minutes after the request that created the booking, on another thread. The trace context is
// kept with the outbox event, the relay sends it as a message attribute, and the consumer's span is a child of the
// request's.
class ContinuesTheTraceOfTheRequestThatScheduledItTest : BookingExpirationConsumerFixture() {
    @Test
    fun `given an event kept inside a trace when the consumer processes its message then it continues that trace`() {
        val tracing = TracingTestSupport()
        val queueUrl = LocalStackSqs.createQueue()
        val request = tracing.tracer.nextSpan().name("http post /bookings").start()
        // what the outbox writer keeps with the event when it is added inside the request
        val headers = mutableMapOf<String, String>()
        tracing.propagator.inject(request.context(), headers) { target, key, value -> target?.put(key, value) }
        request.end()
        val event =
            ClaimedOutboxEvent(
                UUID.randomUUID(),
                "booking.expiration.requested",
                """{"bookingId":$bookingId}""",
                headers,
                1,
            )

        SqsOutboxPublisher(LocalStackSqs.client, OutboxProperties(queues = mapOf("booking-expiration" to queueUrl)))
            .publish(event)
        withTransactionSynchronization { consumerFor(queueUrl, tracing.tracer, tracing.propagator).poll() }

        val consume = tracing.tracer.spans.single { it.name == "booking-expiration consume" }
        assertEquals(request.context().traceId(), consume.context().traceId())
        assertEquals(request.context().spanId(), consume.context().parentId())
    }
}
