package com.dbook.infrastructure.persistence.review

import com.dbook.domain.review.Review

fun ReviewJpaEntity.toDomain(): Review =
    Review(
        id = id,
        customerId = customerId,
        bookingId = bookingId,
        rating = rating,
        comment = comment,
        createdAt = createdAt,
        status = status,
        updatedAt = updatedAt,
        hiddenReason = hiddenReason,
        hiddenBy = hiddenBy,
        hiddenAt = hiddenAt,
    )

fun Review.toJpaEntity(): ReviewJpaEntity =
    ReviewJpaEntity(
        id = id,
        customerId = customerId,
        bookingId = bookingId,
        rating = rating,
        comment = comment,
        createdAt = createdAt,
        status = status,
        updatedAt = updatedAt,
        hiddenReason = hiddenReason,
        hiddenBy = hiddenBy,
        hiddenAt = hiddenAt,
    )
