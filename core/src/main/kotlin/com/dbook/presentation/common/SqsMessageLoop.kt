package com.dbook.presentation.common

import io.micrometer.tracing.Span
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.propagation.Propagator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.Message

/**
 * What every consumer of an SQS queue does the same way: long-poll the queue, handle each message inside a span that
 * continues the trace of whoever sent it, and delete the message **only after it was handled**. A failure leaves the
 * message on the queue, where SQS makes it visible again after the visibility timeout and, after the queue's attempts,
 * moves it to the dead-letter queue; [onFailure] says what to count. Catching everything is deliberate: one poison
 * message must never kill the polling loop.
 */
class SqsMessageLoop(
    private val sqsClient: SqsClient,
    private val queueUrl: String,
    private val waitSeconds: Int,
    private val tracer: Tracer,
    private val propagator: Propagator,
    private val spanName: String,
    private val onFailure: (Message, Exception) -> Unit,
) {
    fun poll(handle: (Message) -> Unit) {
        receive().forEach { process(it, handle) }
    }

    private fun receive(): List<Message> =
        try {
            sqsClient.receiveMessage {
                it.queueUrl(queueUrl)
                    .waitTimeSeconds(waitSeconds)
                    .maxNumberOfMessages(MAX_BATCH)
                    .messageAttributeNames("All")
            }.messages()
        } catch (ex: SdkException) {
            log.error("Could not poll the queue {}", queueUrl, ex)
            emptyList()
        }

    @Suppress("TooGenericExceptionCaught")
    private fun process(
        message: Message,
        handle: (Message) -> Unit,
    ) {
        val span = continuedSpan(message).start()
        try {
            tracer.withSpan(span).use { _ ->
                try {
                    handle(message)
                    sqsClient.deleteMessage { it.queueUrl(queueUrl).receiptHandle(message.receiptHandle()) }
                } catch (ex: Exception) {
                    span.error(ex)
                    onFailure(message, ex)
                    log.error("Failed to process message {} of {}", message.messageId(), spanName, ex)
                }
            }
        } finally {
            span.end()
        }
    }

    // A child of the span that was current when the message was sent (its `traceparent` attribute). A message without
    // one simply starts a trace of its own.
    private fun continuedSpan(message: Message): Span.Builder {
        val carrier = message.messageAttributes().mapValues { (_, attribute) -> attribute.stringValue() }
        return propagator.extract(carrier) { source, key -> source[key] }.name(spanName)
    }

    private companion object {
        const val MAX_BATCH = 10
        val log: Logger = LoggerFactory.getLogger(SqsMessageLoop::class.java)
    }
}
