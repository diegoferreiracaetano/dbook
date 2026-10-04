package com.dbook.presentation.booking.bookingexpirationconsumer

import com.dbook.LocalStackSqs
import com.dbook.TracingTestSupport
import com.dbook.infrastructure.messaging.expiration.SqsBookingExpirationScheduler
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

// The expiration runs minutes after the request that created the booking, on another thread.
// The trace context travels in the message, so the consumer's span is a child of the request's.
class ContinuesTheTraceOfTheRequestThatScheduledItTest : BookingExpirationConsumerFixture() {
    @Test
    fun `given a message scheduled inside a trace when the consumer processes it then it continues that trace`() {
        val tracing = TracingTestSupport()
        val queueUrl = LocalStackSqs.createQueue()
        val scheduler =
            SqsBookingExpirationScheduler(
                LocalStackSqs.client,
                ObjectMapper(),
                tracing.tracer,
                tracing.propagator,
                queueUrl,
            )
        val request = tracing.tracer.nextSpan().name("http post /bookings").start()
        tracing.tracer.withSpan(request).use { scheduler.scheduleExpiration(bookingId, Duration.ZERO) }
        request.end()

        withTransactionSynchronization { consumerFor(queueUrl, tracing.tracer, tracing.propagator).poll() }

        val consume = tracing.tracer.spans.single { it.name == "booking-expiration consume" }
        assertEquals(request.context().traceId(), consume.context().traceId())
        assertEquals(request.context().spanId(), consume.context().parentId())
    }
}
