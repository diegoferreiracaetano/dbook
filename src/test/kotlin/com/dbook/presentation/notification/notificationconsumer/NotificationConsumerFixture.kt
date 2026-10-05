package com.dbook.presentation.notification.notificationconsumer

import com.dbook.LocalStackSqs
import com.dbook.application.pricing.EvaluatePriceAlertsUseCase
import com.dbook.presentation.notification.NotificationConsumer
import com.dbook.presentation.notification.NotificationFixture
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.propagation.Propagator
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import java.util.UUID

// A real (LocalStack) queue feeding the consumer, which hands every message to the real use case and the real tables.
abstract class NotificationConsumerFixture : NotificationFixture() {
    protected val meterRegistry = SimpleMeterRegistry()

    @Autowired
    lateinit var evaluatePriceAlertsUseCase: EvaluatePriceAlertsUseCase

    protected fun consumerFor(queueUrl: String) =
        NotificationConsumer(
            LocalStackSqs.client,
            processNotificationEventUseCase,
            evaluatePriceAlertsUseCase,
            ObjectMapper(),
            meterRegistry,
            Tracer.NOOP,
            Propagator.NOOP,
            queueUrl,
            waitSeconds = 1,
        )

    protected fun send(
        queueUrl: String,
        eventType: String?,
        customerId: Long,
        eventId: UUID = UUID.randomUUID(),
    ) {
        val attributes =
            listOfNotNull(
                "eventId" to eventId.toString(),
                eventType?.let { "eventType" to it },
            ).associate {
                    (name, value) ->
                name to MessageAttributeValue.builder().dataType("String").stringValue(value).build()
            }
        LocalStackSqs.client.sendMessage {
            it.queueUrl(queueUrl)
                .messageBody("""{"customerId":$customerId,"bookingId":1,"title":"GRU-GIG"}""")
                .messageAttributes(attributes)
        }
    }

    protected fun sendRaw(
        queueUrl: String,
        eventType: String,
        eventId: UUID,
        body: String,
    ) {
        val attributes =
            mapOf("eventId" to eventId.toString(), "eventType" to eventType).mapValues { (_, value) ->
                MessageAttributeValue.builder().dataType("String").stringValue(value).build()
            }
        LocalStackSqs.client.sendMessage { it.queueUrl(queueUrl).messageBody(body).messageAttributes(attributes) }
    }

    // longer than the 1 s visibility timeout of the queues: a message that was NOT deleted would be visible again
    protected fun waitForRedelivery() = Thread.sleep(REDELIVERY_WAIT_MS)

    private companion object {
        const val REDELIVERY_WAIT_MS = 2_000L
    }
}
