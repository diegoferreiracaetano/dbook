package com.dbook.domain.flight

import java.math.BigDecimal

/**
 * Who is told, inside the transaction that sets it, whenever a flight gets a price: the price history and the price
 * alerts (the `pricing` concept implements this). The flight does not know who listens, only that someone may.
 */
interface FlightPriceObserver {
    /** [previous] is the price before, or null for a flight that was just created. */
    fun record(
        flight: Flight,
        previous: BigDecimal?,
    )
}
