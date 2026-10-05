package com.dbook.infrastructure.observability

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest
import software.amazon.awssdk.services.sqs.model.QueueAttributeName

/** `dead-letter-queues.queues`: the name of each queue (the `queue` tag of the gauge) and the url of its DLQ. */
@ConfigurationProperties(prefix = "dead-letter-queues")
data class DeadLetterQueueProperties(
    val queues: Map<String, String> = emptyMap(),
)

/**
 * `dbook.sqs.dlq.depth{queue}`: how many messages sit in the dead-letter queue of each queue. A message gets there
 * after failing its attempts, so anything above zero is work that did not get done: a seat that may still be held, a
 * customer who may not have been told.
 *
 * A gauge read from SQS each time Prometheus scrapes (every 15 s), like the pending bookings one is read from the
 * database. When SQS cannot be asked the value is NaN, which Prometheus leaves out: no data, not a false zero. Only
 * where the queue consumers run (they are switched off in the tests, which have no SQS).
 */
@Component
@ConditionalOnProperty(name = ["booking-expiration.consumer.enabled"], havingValue = "true", matchIfMissing = true)
class DeadLetterQueueMetrics(
    private val sqsClient: SqsClient,
    private val properties: DeadLetterQueueProperties,
) : MeterBinder {
    override fun bindTo(registry: MeterRegistry) {
        properties.queues.forEach { (name, url) ->
            Gauge.builder("dbook.sqs.dlq.depth") { depth(url) }
                .tag("queue", name)
                .description("Messages in the dead-letter queue of $name")
                .register(registry)
        }
    }

    private fun depth(queueUrl: String): Double =
        try {
            val request =
                GetQueueAttributesRequest.builder()
                    .queueUrl(queueUrl)
                    .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                    .build()
            sqsClient.getQueueAttributes(request).attributes()[QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES]
                ?.toDoubleOrNull() ?: Double.NaN
        } catch (ex: SdkException) {
            log.warn("Could not read the depth of the dead-letter queue {}", queueUrl, ex)
            Double.NaN
        }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(DeadLetterQueueMetrics::class.java)
    }
}
