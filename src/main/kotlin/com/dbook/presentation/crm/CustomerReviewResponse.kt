package com.dbook.presentation.crm

import com.dbook.domain.crm.CustomerReview
import java.time.LocalDateTime

data class CustomerReviewResponse(
    val id: Long,
    val bookingId: Long,
    val rating: Int,
    val comment: String?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(review: CustomerReview) =
            CustomerReviewResponse(
                id = review.id,
                bookingId = review.bookingId,
                rating = review.rating,
                comment = review.comment,
                createdAt = review.createdAt,
            )
    }
}
