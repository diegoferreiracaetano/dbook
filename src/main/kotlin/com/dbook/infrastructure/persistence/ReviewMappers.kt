package com.dbook.infrastructure.persistence

import com.dbook.domain.Review

fun ReviewJpaEntity.toDomain(): Review =
    Review(
        id = id,
        customerId = customerId,
        bookingId = bookingId,
        rating = rating,
        comment = comment,
        createdAt = createdAt,
    )

fun Review.toJpaEntity(): ReviewJpaEntity =
    ReviewJpaEntity(
        id = id,
        customerId = customerId,
        bookingId = bookingId,
        rating = rating,
        comment = comment,
        createdAt = createdAt,
    )
