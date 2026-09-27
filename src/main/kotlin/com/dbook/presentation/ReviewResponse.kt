package com.dbook.presentation

import com.dbook.domain.Review
import java.time.LocalDateTime

data class ReviewResponse(
    val id: Long? = null,
    val bookingId: Long,
    val customerId: Long,
    val rating: Int,
    val comment: String,
    val createdAt: LocalDateTime,
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
            )
    }
}
