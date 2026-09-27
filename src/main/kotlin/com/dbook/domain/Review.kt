package com.dbook.domain

import java.time.LocalDateTime

private const val MAX_RATING = 5

class Review(
    val id: Long? = null,
    val bookingId: Long,
    val customerId: Long,
    val rating: Int,
    val comment: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    init {
        require(rating in 1..MAX_RATING) { "rating must be between 1 and $MAX_RATING" }
        require(comment.isNotBlank()) { "comment must not be blank" }
    }
}
