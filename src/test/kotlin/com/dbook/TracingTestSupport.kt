package com.dbook

import io.micrometer.tracing.Span
import io.micrometer.tracing.TraceContext
import io.micrometer.tracing.propagation.Propagator
import io.micrometer.tracing.test.simple.SimpleTraceContext
import io.micrometer.tracing.test.simple.SimpleTracer

// An in-memory tracer and a propagator that writes and reads the W3C `traceparent` header, so a
// test can follow a trace across the queue and read back the spans that were recorded.
//
// Deliberately NOT the real OpenTelemetry SDK: OpenTelemetry keeps its context storage global to
// the JVM, and touching it early in a test run stops the Spring contexts that start later from
// putting the traceId in the logs (the real thing is checked live, against Jaeger).
class TracingTestSupport {
    val tracer = SimpleTracer()
    val propagator: Propagator = TraceparentPropagator(tracer)

    private class TraceparentPropagator(private val tracer: SimpleTracer) : Propagator {
        override fun fields(): List<String> = listOf(HEADER)

        override fun <C> inject(
            context: TraceContext,
            carrier: C?,
            setter: Propagator.Setter<C>,
        ) = setter.set(carrier, HEADER, "00-${context.traceId()}-${context.spanId()}-01")

        override fun <C> extract(
            carrier: C,
            getter: Propagator.Getter<C>,
        ): Span.Builder {
            val parts = getter.get(requireNotNull(carrier), HEADER)?.split("-") ?: return tracer.spanBuilder()
            val parent =
                SimpleTraceContext().apply {
                    setTraceId(parts[1])
                    setSpanId(parts[2])
                }
            return tracer.spanBuilder().setParent(parent)
        }
    }

    private companion object {
        const val HEADER = "traceparent"
    }
}
