package com.dbook.presentation.booking

import com.dbook.application.booking.ExpireBookingUseCase
import com.dbook.application.common.countOutcome
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.tracing.Span
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.propagation.Propagator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.Message

/**
 * Receiving side of the booking expiration queue — a driving adapter like a controller,
 * which is why it lives here and may call a use case. A message is deleted only after the
 * booking was handled; a failure leaves it on the queue, where SQS makes it visible again
 * after the visibility timeout and, after maxReceiveCount attempts, moves it to the DLQ.
 */
@Component
@ConditionalOnProperty(name = ["booking-expiration.consumer.enabled"], havingValue = "true", matchIfMissing = true)
class BookingExpirationConsumer(
    private val sqsClient: SqsClient,
    private val expireBookingUseCase: ExpireBookingUseCase,
    private val objectMapper: ObjectMapper,
    private val meterRegistry: MeterRegistry,
    private val tracer: Tracer,
    private val propagator: Propagator,
    @Value("\${booking-expiration.queue-url}") private val queueUrl: String,
    @Value("\${booking-expiration.consumer.wait-seconds:20}") private val waitSeconds: Int,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = POLL_DELAY_MS)
    fun poll() {
        receive().forEach(::process)
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
            log.error("Could not poll the booking expiration queue", ex)
            emptyList()
        }

    // Catching everything is deliberate: one poison message must never kill the polling
    // loop. It is logged and left on the queue, so SQS retries it and finally dead-letters it.
    // The work runs inside a span that continues the trace of the request that scheduled it.
    @Suppress("TooGenericExceptionCaught")
    private fun process(message: Message) {
        val span = continuedSpan(message).start()
        try {
            tracer.withSpan(span).use { _ ->
                try {
                    expireBookingUseCase.execute(bookingIdOf(message.body()))
                    sqsClient.deleteMessage { it.queueUrl(queueUrl).receiptHandle(message.receiptHandle()) }
                } catch (ex: Exception) {
                    span.error(ex)
                    meterRegistry.countOutcome("dbook.booking.expiration", "failed")
                    log.error("Failed to process booking expiration message {}", message.messageId(), ex)
                }
            }
        } finally {
            span.end()
        }
    }

    // A child of the span that was current when the message was sent (its `traceparent`
    // attribute). A message without one simply starts a trace of its own.
    private fun continuedSpan(message: Message): Span.Builder {
        val carrier = message.messageAttributes().mapValues { (_, attribute) -> attribute.stringValue() }
        return propagator.extract(carrier) { source, key -> source[key] }.name("booking-expiration consume")
    }

    // The producer (SqsBookingExpirationScheduler) writes {"bookingId": <n>}; read by field
    // name instead of binding a class — a single-parameter data class is a Jackson trap.
    private fun bookingIdOf(body: String): Long {
        val node = objectMapper.readTree(body).path("bookingId")
        require(node.isIntegralNumber) { "Expiration message without a numeric bookingId: $body" }
        return node.asLong()
    }

    private companion object {
        const val POLL_DELAY_MS = 5_000L
        const val MAX_BATCH = 10
    }
}
