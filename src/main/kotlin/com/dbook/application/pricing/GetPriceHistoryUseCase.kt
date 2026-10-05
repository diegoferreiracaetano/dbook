package com.dbook.application.pricing

import com.dbook.domain.catalog.FlightNotFoundException
import com.dbook.domain.catalog.FlightRepository
import com.dbook.domain.pricing.PriceHistory
import com.dbook.domain.pricing.PricePoint
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import java.math.BigDecimal

data class FlightPriceHistory(
    val flightId: Long,
    val current: BigDecimal,
    val lowest: BigDecimal,
    val highest: BigDecimal,
    val points: List<PricePoint>,
)

/** How a flight's price moved, oldest first, with the lowest and highest it ever had. */
@Observed(name = "dbook.usecase")
@Service
class GetPriceHistoryUseCase(
    private val flightRepository: FlightRepository,
    private val priceHistory: PriceHistory,
) {
    fun execute(flightId: Long): FlightPriceHistory {
        val flight = flightRepository.findById(flightId) ?: throw FlightNotFoundException(flightId)
        val points = priceHistory.of(flightId, MAX_POINTS)
        val prices = points.map { it.price } + flight.price
        return FlightPriceHistory(flightId, flight.price, prices.min(), prices.max(), points)
    }

    private companion object {
        const val MAX_POINTS = 500
    }
}
