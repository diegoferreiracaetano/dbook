package com.dbook.presentation.review

import com.dbook.domain.review.Review
import com.dbook.domain.review.ReviewStatus
import java.time.LocalDateTime

data class ReviewResponse(
    val id: Long? = null,
    val bookingId: Long,
    val customerId: Long,
    val rating: Int,
    val comment: String,
    val createdAt: LocalDateTime,
    val status: ReviewStatus = ReviewStatus.VISIBLE,
    val updatedAt: LocalDateTime? = null,
) {
    companion object {
        fun from(review: Review) =
            ReviewResponse(
                id = review.id,
                customerId = review.customerId,
                bookingId = review.bookingId,
                rating = review.rating,
                comment = review.comment,
                createdAt = review.createdAt,
                status = review.status,
                updatedAt = review.updatedAt,
            )
    }
}
