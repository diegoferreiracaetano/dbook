package com.dbook.presentation.review

import com.dbook.domain.review.DestinationReviews
import com.dbook.domain.review.PublicReview
import com.dbook.presentation.common.PageResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class RatingSummaryResponse(
    @get:Schema(description = "average of the visible reviews, 1 to 5; null when there are none")
    val average: Double?,
    val total: Long,
    @get:Schema(description = "how many reviews gave each rating, keyed 1 to 5")
    val distribution: Map<Int, Long>,
)

data class PublicReviewResponse(
    val id: Long,
    val rating: Int,
    val comment: String,
    @get:Schema(
        example = "Maria S.",
        description = "first name and the initial of the last one; never an e-mail or an id",
    )
    val author: String,
    val createdAt: LocalDateTime,
    val edited: Boolean,
) {
    companion object {
        fun from(review: PublicReview) =
            PublicReviewResponse(
                review.id,
                review.rating,
                review.comment,
                review.author,
                review.createdAt,
                review.edited,
            )
    }
}

data class DestinationReviewsResponse(
    val summary: RatingSummaryResponse,
    val reviews: PageResponse<PublicReviewResponse>,
) {
    companion object {
        fun from(result: DestinationReviews) =
            DestinationReviewsResponse(
                RatingSummaryResponse(result.summary.average, result.summary.total, result.summary.distribution),
                PageResponse.from(result.reviews, PublicReviewResponse::from),
            )
    }
}
