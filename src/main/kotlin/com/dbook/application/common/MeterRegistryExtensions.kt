package com.dbook.application.common

import io.micrometer.core.instrument.MeterRegistry

/**
 * Counts one occurrence of [name], tagged with how it ended ([outcome], e.g. "created" or "replayed").
 * Keep the outcomes, and the extra [tags] (alternating key, value), a short fixed set: every distinct
 * value is a separate time series in Prometheus, so an id or free text must never be used here.
 */
fun MeterRegistry.countOutcome(
    name: String,
    outcome: String,
    vararg tags: String,
) = counter(name, "outcome", outcome, *tags).increment()
