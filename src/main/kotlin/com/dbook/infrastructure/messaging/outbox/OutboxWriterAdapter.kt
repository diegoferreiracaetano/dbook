package com.dbook.infrastructure.messaging.outbox

import com.dbook.domain.messaging.OutboxEvent
import com.dbook.domain.messaging.OutboxWriter
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.propagation.Propagator
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Clock
import java.util.UUID

@Component
class OutboxWriterAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val objectMapper: ObjectMapper,
    private val tracer: Tracer,
    private val propagator: Propagator,
    private val clock: Clock,
) : OutboxWriter {
    // MANDATORY: an event written outside the transaction of its change is exactly what the outbox must never allow
    @Transactional(propagation = Propagation.MANDATORY)
    override fun add(event: OutboxEvent) {
        val now = Timestamp.from(clock.instant())
        jdbc.update(
            "INSERT INTO outbox_event (id, aggregate_type, aggregate_id, type, payload, headers, created_at, " +
                "available_at, next_attempt_at) VALUES (:id, :aggregateType, :aggregateId, :type, " +
                "CAST(:payload AS jsonb), CAST(:headers AS jsonb), :createdAt, :availableAt, :availableAt)",
            mapOf(
                "id" to UUID.randomUUID(),
                "aggregateType" to event.aggregateType,
                "aggregateId" to event.aggregateId,
                "type" to event.type,
                "payload" to objectMapper.writeValueAsString(event.payload),
                "headers" to objectMapper.writeValueAsString(traceHeaders()),
                "createdAt" to now,
                "availableAt" to Timestamp.from(event.availableAt),
            ),
        )
    }

    // The delivery happens minutes later, on another thread: the trace context of the request that caused the event
    // is kept with it, so the consumer continues the SAME trace. Without a current span there is nothing to keep.
    private fun traceHeaders(): Map<String, String> {
        val carrier = mutableMapOf<String, String>()
        tracer.currentSpan()?.let { span ->
            propagator.inject(span.context(), carrier) { target, key, value -> target?.put(key, value) }
        }
        return carrier
    }
}
