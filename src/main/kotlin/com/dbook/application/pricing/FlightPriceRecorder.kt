package com.dbook.application.pricing

import com.dbook.domain.catalog.Flight
import com.dbook.domain.catalog.FlightEvents
import com.dbook.domain.messaging.OutboxWriter
import com.dbook.domain.pricing.PriceHistory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Clock

/**
 * What must happen whenever a flight gets a price, inside the transaction that sets it: the price joins the history
 * and a `flight.price-changed` event goes to the outbox (the price alerts listen to it). The same price again is not
 * a change.
 */
@Service
class FlightPriceRecorder(
    private val priceHistory: PriceHistory,
    private val outboxWriter: OutboxWriter,
    private val clock: Clock,
) {
    /** [previous] is the price before, or null for a flight that was just created. */
    fun record(
        flight: Flight,
        previous: BigDecimal?,
    ) {
        if (previous != null && flight.price.compareTo(previous) == 0) {
            return
        }
        val now = clock.instant()
        priceHistory.record(requireNotNull(flight.id), flight.price, now)
        outboxWriter.add(FlightEvents.priceChanged(flight, previous, now))
    }
}
