package com.dbook.application

import com.dbook.domain.ReviewRepository
import org.springframework.stereotype.Service

/**
 * Average rating (1-5) among every review for flights to a destination — no authentication
 * required. Backs the rating shown on destination cards, mirror of
 * [GetLowestPriceForDestinationUseCase].
 */
@Service
class GetAverageRatingForDestinationUseCase(
    private val reviewRepository: ReviewRepository,
) {
    fun execute(destinationIataCode: String): Double? =
        reviewRepository.findAverageRatingByDestination(destinationIataCode)
}
