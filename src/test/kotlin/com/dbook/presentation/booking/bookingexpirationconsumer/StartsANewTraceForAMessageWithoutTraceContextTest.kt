package com.dbook.presentation.booking.bookingexpirationconsumer

import com.dbook.LocalStackSqs
import com.dbook.TracingTestSupport
import io.opentelemetry.api.trace.SpanId
import kotlin.test.Test
import kotlin.test.assertEquals

class StartsANewTraceForAMessageWithoutTraceContextTest : BookingExpirationConsumerFixture() {
    @Test
    fun `given a message with no trace context when the consumer processes it then it starts a trace of its own`() {
        val tracing = TracingTestSupport()
        val queueUrl = LocalStackSqs.createQueue()
        LocalStackSqs.send(queueUrl, """{"bookingId":$bookingId}""")

        withTransactionSynchronization { consumerFor(queueUrl, tracing.tracer, tracing.propagator).poll() }

        val consume = tracing.exporter.finishedSpanItems.single { it.name == "booking-expiration consume" }
        assertEquals(SpanId.getInvalid(), consume.parentSpanId)
    }
}
