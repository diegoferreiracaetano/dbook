package com.dbook.infrastructure.observability

import com.dbook.domain.booking.BookingRepository
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.springframework.stereotype.Component

/**
 * `dbook.booking.pending`: how many bookings are holding a seat while waiting for payment.
 *
 * A gauge, unlike the counters: it is the current value, read from the database each time
 * Prometheus scrapes. If it only ever grows, seats are getting stuck and the expiration queue
 * is not doing its job — that is what to alert on.
 */
@Component
class PendingBookingsMetrics(
    private val bookingRepository: BookingRepository,
) : MeterBinder {
    override fun bindTo(registry: MeterRegistry) {
        Gauge.builder("dbook.booking.pending") { bookingRepository.countPending().toDouble() }
            .description("Bookings holding a seat while they wait for payment")
            .register(registry)
    }
}
