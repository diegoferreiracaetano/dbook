package com.dbook.application.review

import com.dbook.domain.accommodation.AccommodationNotFoundException
import com.dbook.domain.accommodation.AccommodationRepository
import com.dbook.domain.common.PageQuery
import com.dbook.domain.review.DestinationReviews
import com.dbook.domain.review.PublicReviewReader
import com.dbook.domain.review.ReviewSort
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service

/** What a hotel's guests say about it: how it is rated and the visible reviews, one page at a time. No sign-in. */
@Observed(name = "dbook.usecase")
@Service
class ListAccommodationReviewsUseCase(
    private val publicReviewReader: PublicReviewReader,
    private val accommodations: AccommodationRepository,
) {
    fun execute(
        accommodationId: Long,
        sort: ReviewSort,
        page: PageQuery,
    ): DestinationReviews {
        val hotel = accommodations.findById(accommodationId)
        if (hotel == null || !hotel.active) throw AccommodationNotFoundException(accommodationId)
        return publicReviewReader.ofAccommodation(accommodationId, sort, page)
    }
}
