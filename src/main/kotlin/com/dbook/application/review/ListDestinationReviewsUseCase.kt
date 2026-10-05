package com.dbook.application.review

import com.dbook.domain.common.PageQuery
import com.dbook.domain.review.DestinationReviews
import com.dbook.domain.review.PublicReviewReader
import com.dbook.domain.review.ReviewSort
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

/** What a destination's visitors read: how it is rated and the visible reviews, one page at a time. No sign-in. */
@Observed(name = "dbook.usecase")
@Service
class ListDestinationReviewsUseCase(
    private val publicReviewReader: PublicReviewReader,
) {
    fun execute(
        iataCode: String,
        sort: ReviewSort,
        page: PageQuery,
    ): DestinationReviews {
        require(iataCode.length == IATA_LENGTH && iataCode.all { it.isLetter() }) {
            "iataCode must have $IATA_LENGTH letters"
        }
        return publicReviewReader.ofDestination(iataCode.uppercase(), sort, page)
    }

    private companion object {
        const val IATA_LENGTH = 3
    }
}
