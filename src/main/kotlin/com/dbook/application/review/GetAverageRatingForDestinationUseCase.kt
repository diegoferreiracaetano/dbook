package com.dbook.application.review

import com.dbook.domain.review.ReviewRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

/**
 * Average rating (1-5) among every review for flights to a destination — no authentication
 * required. Backs the rating shown on destination cards, mirror of
 * [GetLowestPriceForDestinationUseCase].
 */
@Observed(name = "dbook.usecase")
@Service
class GetAverageRatingForDestinationUseCase(
    private val reviewRepository: ReviewRepository,
) {
    fun execute(destinationIataCode: String): Double? =
        reviewRepository.findAverageRatingByDestination(destinationIataCode)
}
