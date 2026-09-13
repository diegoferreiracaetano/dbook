package com.dbook.application

import com.dbook.domain.Airport
import com.dbook.domain.AirportRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal

data class FeaturedDestination(
    val airport: Airport,
    val lowestPrice: BigDecimal?,
)

/**
 * Every known destination with its real lowest price — no authentication required. Reuses
 * [GetLowestPriceForDestinationUseCase] (M12) per airport instead of duplicating the price
 * aggregation; backs `GET /destinations`, the one call the mobile Home screen needs instead of
 * a hardcoded airport list plus one price request per card.
 */
@Service
class GetFeaturedDestinationsUseCase(
    private val airportRepository: AirportRepository,
    private val getLowestPriceForDestinationUseCase: GetLowestPriceForDestinationUseCase,
) {
    fun execute(): List<FeaturedDestination> =
        airportRepository.findAll().map { airport ->
            FeaturedDestination(
                airport = airport,
                lowestPrice = getLowestPriceForDestinationUseCase.execute(airport.iataCode),
            )
        }
}
