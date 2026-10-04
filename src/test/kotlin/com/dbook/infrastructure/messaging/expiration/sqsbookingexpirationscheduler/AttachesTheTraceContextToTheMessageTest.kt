package com.dbook.infrastructure.messaging.expiration.sqsbookingexpirationscheduler

import com.dbook.LocalStackSqs
import com.dbook.TracingTestSupport
import com.dbook.infrastructure.messaging.expiration.SqsBookingExpirationScheduler
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AttachesTheTraceContextToTheMessageTest : SqsBookingExpirationSchedulerFixture() {
    @Test
    fun `given a current span when scheduling then the message carries its trace context, and none without a span`() {
        val tracing = TracingTestSupport()
        val withTrace = createQueue()
        val withoutTrace = createQueue()
        val scheduler = { queue: String ->
            SqsBookingExpirationScheduler(
                LocalStackSqs.client,
                ObjectMapper(),
                tracing.tracer,
                tracing.propagator,
                queue,
            )
        }

        val request = tracing.tracer.nextSpan().name("http post /bookings").start()
        tracing.tracer.withSpan(request).use { scheduler(withTrace).scheduleExpiration(1L, Duration.ZERO) }
        request.end()
        scheduler(withoutTrace).scheduleExpiration(1L, Duration.ZERO)

        val traceparent = LocalStackSqs.receiveWithAttributes(withTrace, 5).single().messageAttributes()["traceparent"]
        assertTrue(traceparent != null && request.context().traceId() in traceparent.stringValue(), "no traceparent")
        assertNull(LocalStackSqs.receiveWithAttributes(withoutTrace, 5).single().messageAttributes()["traceparent"])
    }
}
