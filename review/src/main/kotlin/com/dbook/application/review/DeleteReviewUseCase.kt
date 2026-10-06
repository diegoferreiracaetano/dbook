package com.dbook.application.review

import com.dbook.domain.review.NotReviewOwnerException
import com.dbook.domain.review.ReviewNotFoundException
import com.dbook.domain.review.ReviewRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** The author takes their review down for good (its reports go with it). The booking can be reviewed again. */
@Observed(name = "dbook.usecase")
@Service
class DeleteReviewUseCase(
    private val reviewRepository: ReviewRepository,
) {
    @Transactional
    fun execute(
        reviewId: Long,
        customerId: Long,
    ) {
        val review = reviewRepository.findById(reviewId) ?: throw ReviewNotFoundException(reviewId)
        if (review.customerId != customerId) {
            throw NotReviewOwnerException(reviewId)
        }
        reviewRepository.delete(reviewId)
    }
}
