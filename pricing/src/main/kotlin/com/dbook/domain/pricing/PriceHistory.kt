package com.dbook.domain.pricing

import java.math.BigDecimal
import java.time.Instant

/** The price a flight had from [changedAt] on. */
data class PricePoint(
    val price: BigDecimal,
    val changedAt: Instant,
)

interface PriceHistory {
    fun record(
        flightId: Long,
        price: BigDecimal,
        at: Instant,
    )

    /** The most recent [limit] points, oldest first. */
    fun of(
        flightId: Long,
        limit: Int,
    ): List<PricePoint>
}
