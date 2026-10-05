package com.dbook.infrastructure.persistence.review

import com.dbook.domain.review.Review
import com.dbook.domain.review.ReviewRepository
import org.springframework.stereotype.Repository

@Repository
class ReviewRepositoryAdapter(
    private val reviewJpaRepository: ReviewJpaRepository,
) : ReviewRepository {
    override fun findById(id: Long): Review? = reviewJpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByBookingId(bookingId: Long): Review? = reviewJpaRepository.findByBookingId(bookingId)?.toDomain()

    override fun findByBookingIds(bookingIds: Collection<Long>): List<Review> =
        reviewJpaRepository.findByBookingIdIn(bookingIds).map { it.toDomain() }

    override fun save(review: Review): Review = reviewJpaRepository.save(review.toJpaEntity()).toDomain()

    override fun delete(id: Long) = reviewJpaRepository.deleteById(id)

    override fun findAverageRatingByDestination(destinationIataCode: String) =
        reviewJpaRepository.findAverageRatingByDestination(destinationIataCode)
}
