package com.dbook.infrastructure.messaging.outbox

import com.dbook.TracingTestSupport
import com.dbook.domain.messaging.OutboxEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TheEventKeepsItsPayloadItsDueTimeAndTheTraceTest : OutboxFixture() {
    private fun header(type: String) =
        jdbcTemplate.queryForObject("SELECT headers::text FROM outbox_event WHERE type = ?", String::class.java, type)

    @Test
    fun `given a current span when adding an event then its trace travels with it, and nothing without one`() {
        val tracing = TracingTestSupport()
        val adapter =
            OutboxWriterAdapter(
                NamedParameterJdbcTemplate(jdbcTemplate),
                ObjectMapper(),
                tracing.tracer,
                tracing.propagator,
                clock,
            )
        val due = Instant.parse("2031-01-01T10:00:00Z")
        val span = tracing.tracer.nextSpan().name("http post /bookings").start()
        inTransaction {
            tracing.tracer.withSpan(span).use {
                adapter.add(OutboxEvent("booking", "7", "test.traced.event", mapOf("bookingId" to 7), due))
            }
            adapter.add(OutboxEvent("booking", "8", "test.untraced.event", mapOf("bookingId" to 8), due))
        }
        span.end()

        assertTrue(span.context().traceId() in header("test.traced.event").orEmpty())
        assertEquals("{}", header("test.untraced.event"))
        assertEquals(
            """{"bookingId": 7}""",
            jdbcTemplate.queryForObject(
                "SELECT payload::text FROM outbox_event WHERE type = 'test.traced.event'",
                String::class.java,
            ),
        )
        assertEquals(
            due.toEpochMilli(),
            jdbcTemplate.queryForObject(
                "SELECT (extract(epoch FROM available_at) * 1000)::bigint " +
                    "FROM outbox_event WHERE type = 'test.traced.event'",
                Long::class.java,
            ),
        )
    }
}
