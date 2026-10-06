package com.dbook.application.flight

import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.AirportRepository
import com.dbook.domain.flight.DestinationRatings
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import java.math.BigDecimal

data class FeaturedDestination(
    val airport: Airport,
    val lowestPrice: BigDecimal?,
    val averageRating: Double?,
)

/**
 * Every known destination with its real lowest price — no authentication required. Reuses
 * [GetLowestPriceForDestinationUseCase] (M12) per airport instead of duplicating the price
 * aggregation; backs `GET /destinations`, the one call the mobile Home screen needs instead of
 * a hardcoded airport list plus one price request per card.
 */
@Observed(name = "dbook.usecase")
@Service
class GetFeaturedDestinationsUseCase(
    private val airportRepository: AirportRepository,
    private val getLowestPriceForDestinationUseCase: GetLowestPriceForDestinationUseCase,
    private val destinationRatings: DestinationRatings,
) {
    fun execute(): List<FeaturedDestination> =
        airportRepository.findAll().map { airport ->
            FeaturedDestination(
                airport = airport,
                lowestPrice = getLowestPriceForDestinationUseCase.execute(airport.iataCode),
                averageRating = destinationRatings.averageRating(airport.iataCode),
            )
        }
}
