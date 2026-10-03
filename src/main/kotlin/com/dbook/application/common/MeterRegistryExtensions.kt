package com.dbook.application.common

import io.micrometer.core.instrument.MeterRegistry

/**
 * Counts one occurrence of [name], tagged with how it ended ([outcome], e.g. "created" or
 * "replayed"). Keep the outcomes a short fixed set: every distinct value is a separate time
 * series in Prometheus, so an id or any free text must never be used here.
 */
fun MeterRegistry.countOutcome(
    name: String,
    outcome: String,
) = counter(name, "outcome", outcome).increment()
