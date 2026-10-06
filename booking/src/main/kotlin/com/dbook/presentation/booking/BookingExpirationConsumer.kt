package com.dbook.presentation.booking

import com.dbook.application.booking.ExpireBookingUseCase
import com.dbook.application.common.countOutcome
import com.dbook.presentation.common.SqsMessageLoop
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.propagation.Propagator
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient

/**
 * Receiving side of the booking expiration queue — a driving adapter like a controller, which is why it lives here and
 * may call a use case. The polling, the trace and the delete-only-after-handled rule are in [SqsMessageLoop].
 */
@Component
@ConditionalOnProperty(name = ["booking-expiration.consumer.enabled"], havingValue = "true", matchIfMissing = true)
class BookingExpirationConsumer(
    sqsClient: SqsClient,
    private val expireBookingUseCase: ExpireBookingUseCase,
    private val objectMapper: ObjectMapper,
    meterRegistry: MeterRegistry,
    tracer: Tracer,
    propagator: Propagator,
    @Value("\${booking-expiration.queue-url}") queueUrl: String,
    @Value("\${booking-expiration.consumer.wait-seconds:20}") waitSeconds: Int,
) {
    private val loop =
        SqsMessageLoop(sqsClient, queueUrl, waitSeconds, tracer, propagator, "booking-expiration consume") { _, _ ->
            meterRegistry.countOutcome("dbook.booking.expiration", "failed")
        }

    @Scheduled(fixedDelay = POLL_DELAY_MS)
    fun poll() {
        loop.poll { expireBookingUseCase.execute(bookingIdOf(it.body())) }
    }

    // The producer (the outbox relay, for a booking.expiration.requested event) writes {"bookingId": <n>}; read by
    // field name instead of binding a class — a single-parameter data class is a Jackson trap.
    private fun bookingIdOf(body: String): Long {
        val node = objectMapper.readTree(body).path("bookingId")
        require(node.isIntegralNumber) { "Expiration message without a numeric bookingId: $body" }
        return node.asLong()
    }

    private companion object {
        const val POLL_DELAY_MS = 5_000L
    }
}
