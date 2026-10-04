package com.dbook.infrastructure.observability.pendingbookingsmetrics

import com.dbook.domain.booking.Booking
import com.dbook.domain.booking.BookingRepository
import com.dbook.infrastructure.observability.PendingBookingsMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportsTheCurrentNumberOfPendingBookingsTest {
    // only countPending() matters here; the gauge must not touch anything else
    private class PendingCount(var pending: Long) : BookingRepository {
        override fun findById(id: Long): Booking? = error("not used by the gauge")

        override fun findByCustomerId(customerId: Long): List<Booking> = error("not used by the gauge")

        override fun save(booking: Booking): Booking = error("not used by the gauge")

        override fun countPending(): Long = pending
    }

    @Test
    fun `given a gauge when the pending count changes then each read reports the value at that moment`() {
        val repository = PendingCount(pending = 3)
        val registry = SimpleMeterRegistry()
        PendingBookingsMetrics(repository).bindTo(registry)
        val gauge = registry.get("dbook.booking.pending").gauge()

        assertEquals(3.0, gauge.value())

        repository.pending = 1

        assertEquals(1.0, gauge.value())
    }
}
