package com.dbook.presentation.booking.bookingexpirationconsumer

import com.dbook.LocalStackSqs
import com.dbook.TracingTestSupport
import kotlin.test.Test
import kotlin.test.assertTrue

class StartsANewTraceForAMessageWithoutTraceContextTest : BookingExpirationConsumerFixture() {
    @Test
    fun `given a message with no trace context when the consumer processes it then it starts a trace of its own`() {
        val tracing = TracingTestSupport()
        val queueUrl = LocalStackSqs.createQueue()
        LocalStackSqs.send(queueUrl, """{"bookingId":$bookingId}""")

        withTransactionSynchronization { consumerFor(queueUrl, tracing.tracer, tracing.propagator).poll() }

        val consume = tracing.tracer.spans.single { it.name == "booking-expiration consume" }
        assertTrue(consume.context().parentId().isNullOrEmpty(), "unexpected parent: ${consume.context().parentId()}")
    }
}
