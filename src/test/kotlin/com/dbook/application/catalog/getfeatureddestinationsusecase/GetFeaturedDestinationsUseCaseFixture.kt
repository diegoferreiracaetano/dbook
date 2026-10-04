package com.dbook.application.catalog.getfeatureddestinationsusecase

import com.dbook.application.catalog.GetFeaturedDestinationsUseCase
import com.dbook.application.catalog.GetLowestPriceForDestinationUseCase
import com.dbook.application.review.GetAverageRatingForDestinationUseCase
import com.dbook.application.review.createreviewusecase.FakeReviewRepository
import com.dbook.domain.catalog.Airport
import com.dbook.domain.catalog.AirportRepository
import com.dbook.domain.catalog.Flight
import com.dbook.domain.catalog.FlightRepository
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

class FakeAirportRepository(private val airports: List<Airport>) : AirportRepository {
    override fun findByIataCode(iataCode: String): Airport? = airports.find { it.iataCode == iataCode }

    override fun findAll(): List<Airport> = airports
}

class FakeFlightRepository(private val lowestPriceByDestination: Map<String, BigDecimal>) : FlightRepository {
    override fun findById(id: Long): Flight? = error("not used by GetFeaturedDestinationsUseCase")

    override fun save(flight: Flight): Flight = error("not used by GetFeaturedDestinationsUseCase")

    override fun search(
        originIataCode: String,
        destinationIataCode: String,
        date: LocalDate,
    ): List<Flight> = error("not used by GetFeaturedDestinationsUseCase")

    override fun findActive(): List<Flight> = error("not used by GetFeaturedDestinationsUseCase")

    override fun findLowestPrice(
        destinationIataCode: String,
        from: LocalDate,
        to: LocalDate,
    ): BigDecimal? = lowestPriceByDestination[destinationIataCode]
}

// Shared "given": each scenario below supplies its own airport list and price map.
abstract class GetFeaturedDestinationsUseCaseFixture {
    protected fun useCase(
        airports: List<Airport>,
        prices: Map<String, BigDecimal> = emptyMap(),
        ratings: Map<String, Double> = emptyMap(),
    ) = GetFeaturedDestinationsUseCase(
        FakeAirportRepository(airports),
        GetLowestPriceForDestinationUseCase(FakeFlightRepository(prices), Clock.systemDefaultZone()),
        GetAverageRatingForDestinationUseCase(FakeReviewRepository(averageRatingByDestination = ratings)),
    )

    protected fun airport(iataCode: String) =
        Airport(
            id = 1,
            iataCode = iataCode,
            name = iataCode,
            city = iataCode,
            country = iataCode,
            photoUrl = "https://example.com/$iataCode.jpg",
            region = "América do Sul",
            isPopular = false,
        )
}
