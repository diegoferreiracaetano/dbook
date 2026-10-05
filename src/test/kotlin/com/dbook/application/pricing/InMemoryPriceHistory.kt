package com.dbook.application.pricing

import com.dbook.domain.pricing.PriceHistory
import com.dbook.domain.pricing.PricePoint
import java.math.BigDecimal
import java.time.Instant

class InMemoryPriceHistory : PriceHistory {
    val rows = mutableListOf<Triple<Long, BigDecimal, Instant>>()

    override fun record(
        flightId: Long,
        price: BigDecimal,
        at: Instant,
    ) {
        rows += Triple(flightId, price, at)
    }

    override fun of(
        flightId: Long,
        limit: Int,
    ): List<PricePoint> =
        rows.filter { it.first == flightId }.sortedBy { it.third }.takeLast(
            limit,
        ).map { PricePoint(it.second, it.third) }
}
