package com.dbook

import io.micrometer.tracing.Tracer
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext
import io.micrometer.tracing.otel.bridge.OtelPropagator
import io.micrometer.tracing.otel.bridge.OtelTracer
import io.micrometer.tracing.propagation.Propagator
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor

// A real OpenTelemetry tracer and W3C propagator (what the application uses), exporting to
// memory instead of a collector, so a test can follow a trace across the queue and read back
// the spans that were recorded.
class TracingTestSupport {
    val exporter: InMemorySpanExporter = InMemorySpanExporter.create()

    private val propagators = ContextPropagators.create(W3CTraceContextPropagator.getInstance())
    private val otelTracer =
        OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(exporter)).build(),
            )
            .setPropagators(propagators)
            .build()
            .getTracer("test")

    val tracer: Tracer = OtelTracer(otelTracer, OtelCurrentTraceContext()) { }
    val propagator: Propagator = OtelPropagator(propagators, otelTracer)
}
