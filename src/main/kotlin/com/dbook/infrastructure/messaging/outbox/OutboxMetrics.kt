package com.dbook.infrastructure.messaging.outbox

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.springframework.stereotype.Component
import java.time.Clock

/**
 * `dbook.outbox.pending`: events not yet published, the ones scheduled for later included (every booking made in the
 * last 15 minutes has one, which is normal). `dbook.outbox.overdue.seconds`: how late the most overdue event is, 0 when
 * none is. That one is what to alert on: events that should have gone out and have not.
 */
@Component
class OutboxMetrics(
    private val store: OutboxStore,
    private val clock: Clock,
) : MeterBinder {
    override fun bindTo(registry: MeterRegistry) {
        Gauge.builder("dbook.outbox.pending") { store.stats(clock.instant()).pending.toDouble() }
            .description("Outbox events not yet published, including the ones scheduled for later")
            .register(registry)
        Gauge.builder("dbook.outbox.overdue.seconds") { store.stats(clock.instant()).overdueSeconds.toDouble() }
            .description("How late the most overdue outbox event is, in seconds (0 when none is overdue)")
            .register(registry)
    }
}
