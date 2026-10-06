package com.dbook.application.review

import com.dbook.domain.review.NotReviewOwnerException
import com.dbook.domain.review.Review
import com.dbook.domain.review.ReviewNotFoundException
import com.dbook.domain.review.ReviewRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

data class EditReviewCommand(
    val reviewId: Long,
    val customerId: Long,
    val rating: Int?,
    val comment: String?,
)

/** The author changes the rating, the comment or both. Anyone else gets a 403; the average follows on its own. */
@Observed(name = "dbook.usecase")
@Service
class EditReviewUseCase(
    private val reviewRepository: ReviewRepository,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: EditReviewCommand): Review {
        val review = reviewRepository.findById(command.reviewId) ?: throw ReviewNotFoundException(command.reviewId)
        if (review.customerId != command.customerId) {
            throw NotReviewOwnerException(command.reviewId)
        }
        return reviewRepository.save(review.edit(command.rating, command.comment, LocalDateTime.now(clock)))
    }
}
