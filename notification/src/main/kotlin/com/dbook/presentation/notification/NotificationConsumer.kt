package com.dbook.presentation.notification

import com.dbook.application.notification.NotificationEvent
import com.dbook.application.notification.ProcessNotificationEventUseCase
import com.dbook.application.pricing.EvaluatePriceAlertsUseCase
import com.dbook.domain.flight.FlightEvents
import com.dbook.domain.notification.NotificationType
import com.dbook.domain.pricing.PriceChange
import com.dbook.presentation.common.SqsMessageLoop
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.propagation.Propagator
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.Message
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * Receiving side of the notifications queue. The outbox relay sends each domain event with its id (`eventId`) and type
 * (`eventType`) as message attributes; the id is what makes a duplicate delivery harmless. A message of a type nobody
 * here knows, or one that cannot be read, is a failure on purpose: it goes to the dead-letter queue, where it is seen,
 * instead of being thrown away.
 */
@Component
@ConditionalOnProperty(name = ["notifications.consumer.enabled"], havingValue = "true", matchIfMissing = true)
class NotificationConsumer(
    sqsClient: SqsClient,
    private val processNotificationEventUseCase: ProcessNotificationEventUseCase,
    private val evaluatePriceAlertsUseCase: EvaluatePriceAlertsUseCase,
    private val objectMapper: ObjectMapper,
    private val meterRegistry: MeterRegistry,
    tracer: Tracer,
    propagator: Propagator,
    @Value("\${notifications.queue-url}") queueUrl: String,
    @Value("\${notifications.consumer.wait-seconds:20}") waitSeconds: Int,
) {
    private val loop =
        SqsMessageLoop(sqsClient, queueUrl, waitSeconds, tracer, propagator, "notifications consume") { _, _ ->
            meterRegistry.counter("dbook.notification.messages", "outcome", "failed").increment()
        }

    @Scheduled(fixedDelay = POLL_DELAY_MS)
    fun poll() {
        loop.poll { handle(it) }
    }

    // What arrives on this queue is a fact about a booking, a refund or a flight, or a flight's new price: the price
    // is not told to anyone, it is checked against the price alerts, which then produce the notifications.
    private fun handle(message: Message) {
        val eventType =
            requireNotNull(message.messageAttributes()["eventType"]?.stringValue()) { "Message without an eventType" }
        if (eventType == FlightEvents.PRICE_CHANGED) {
            evaluatePriceAlertsUseCase.execute(priceChangeOf(message))
        } else {
            processNotificationEventUseCase.execute(eventOf(message, eventType))
        }
    }

    private fun priceChangeOf(message: Message): PriceChange {
        val data = objectMapper.readTree(message.body())
        return PriceChange(
            flightId = data.path("flightId").asLong(),
            flightNumber = data.path("flightNumber").asText(),
            origin = data.path("origin").asText(),
            destination = data.path("destination").asText(),
            travelDate = LocalDate.parse(data.path("date").asText()),
            price = BigDecimal(data.path("price").asText()),
        )
    }

    private fun eventOf(
        message: Message,
        eventType: String,
    ): NotificationEvent {
        val attributes = message.messageAttributes()
        val type =
            requireNotNull(NotificationType.fromEventType(eventType)) { "Unknown notification event: $eventType" }
        val eventId =
            UUID.fromString(
                requireNotNull(attributes["eventId"]?.stringValue()) { "Message without an eventId" },
            )
        return NotificationEvent(eventId, type, objectMapper.readValue(message.body(), DATA))
    }

    private companion object {
        const val POLL_DELAY_MS = 5_000L
        val DATA = object : TypeReference<Map<String, Any?>>() {}
    }
}
