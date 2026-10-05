package com.dbook.infrastructure.messaging.outbox

import org.springframework.stereotype.Component
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue

/**
 * Sends an event to its SQS queue, with no delay: the outbox already held it until it was due. The headers (the trace
 * context), the event's id and its type go as message attributes, which is where the consumers read them. A type with
 * no route is a failure, so a misconfiguration shows up as events that do not leave (and an alert) instead of being
 * dropped.
 */
@Component
class SqsOutboxPublisher(
    private val sqsClient: SqsClient,
    private val properties: OutboxProperties,
) : OutboxPublisher {
    override fun publish(event: ClaimedOutboxEvent) {
        val queueUrl =
            properties.queues[routingKeyOf(event.type)]
                ?: throw OutboxPublishException("No queue is configured for events of type ${event.type}")
        try {
            sqsClient.sendMessage {
                it.queueUrl(queueUrl)
                    .messageBody(event.payload)
                    .messageAttributes(
                        // the id lets a consumer recognise a duplicate; the type tells it what the payload is
                        (event.headers + mapOf("eventId" to event.id.toString(), "eventType" to event.type)).mapValues {
                                (_, value) ->
                            MessageAttributeValue.builder().dataType("String").stringValue(value).build()
                        },
                    )
            }
        } catch (ex: SdkException) {
            throw OutboxPublishException("Could not send ${event.type} to its queue", ex)
        }
    }

    private fun routingKeyOf(type: String): String = type.substringBeforeLast('.').replace('.', '-')
}
