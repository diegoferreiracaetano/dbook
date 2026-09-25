package com.dbook.infrastructure.persistence

import com.dbook.domain.Review
import com.dbook.domain.ReviewRepository
import org.springframework.stereotype.Repository

@Repository
class ReviewRepositoryAdapter(
    private val reviewJpaRepository: ReviewJpaRepository,
) : ReviewRepository {
    override fun findById(id: Long): Review? = reviewJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByBookingId(bookingId: Long): Review? = reviewJpaRepository.findByBookingId(bookingId)?.toDomain()

    override fun save(review: Review): Review = reviewJpaRepository.save(review.toJpaEntity()).toDomain()
}
