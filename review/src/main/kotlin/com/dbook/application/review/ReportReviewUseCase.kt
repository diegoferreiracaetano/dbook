package com.dbook.application.review

import com.dbook.domain.review.ReviewNotFoundException
import com.dbook.domain.review.ReviewReport
import com.dbook.domain.review.ReviewReportRepository
import com.dbook.domain.review.ReviewRepository
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

data class ReportReviewCommand(
    val reviewId: Long,
    val reporterId: Long,
    val reason: String,
)

/**
 * A customer flags a review for the team. A hidden review does not exist for them (404, like one that is not there),
 * you cannot report your own, and each customer reports a review once (a second report is a 409).
 */
@Observed(name = "dbook.usecase")
@Service
class ReportReviewUseCase(
    private val reviewRepository: ReviewRepository,
    private val reportRepository: ReviewReportRepository,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: ReportReviewCommand) {
        val review =
            reviewRepository.findById(command.reviewId)?.takeIf { it.isVisible }
                ?: throw ReviewNotFoundException(command.reviewId)
        check(review.customerId != command.reporterId) { "You cannot report your own review" }

        val added =
            reportRepository.add(
                ReviewReport(command.reviewId, command.reporterId, command.reason, LocalDateTime.now(clock)),
            )
        check(added) { "You already reported review ${command.reviewId}" }
    }
}
