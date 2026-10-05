package com.dbook.infrastructure.observability

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TheDeadLetterQueueDepthIsAGaugeTest {
    private val sqs: SqsClient = mock(SqsClient::class.java)
    private val registry = SimpleMeterRegistry()

    private fun gaugeValue(): Double {
        DeadLetterQueueMetrics(
            sqs,
            DeadLetterQueueProperties(mapOf("booking-expiration" to "http://sqs/dlq")),
        ).bindTo(registry)
        return registry.get("dbook.sqs.dlq.depth").tag("queue", "booking-expiration").gauge().value()
    }

    @Test
    fun `given 3 messages in the dead-letter queue when the gauge is read then it is 3`() {
        `when`(sqs.getQueueAttributes(any(GetQueueAttributesRequest::class.java))).thenReturn(
            GetQueueAttributesResponse.builder()
                .attributes(mapOf(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES to "3"))
                .build(),
        )

        assertEquals(3.0, gaugeValue())
    }

    @Test
    fun `given SQS that cannot be asked when the gauge is read then it is NaN, not a false zero`() {
        `when`(sqs.getQueueAttributes(any(GetQueueAttributesRequest::class.java)))
            .thenThrow(SdkClientException.create("unreachable"))

        assertTrue(gaugeValue().isNaN())
    }
}
