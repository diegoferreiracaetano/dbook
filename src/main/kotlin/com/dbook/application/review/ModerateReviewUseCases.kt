package com.dbook.application.review

import com.dbook.domain.audit.AuditAction
import com.dbook.domain.audit.AuditEvent
import com.dbook.domain.audit.AuditLog
import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import com.dbook.domain.identity.Actor
import com.dbook.domain.review.AdminReviewReader
import com.dbook.domain.review.AdminReviewStatus
import com.dbook.domain.review.AdminReviewSummary
import com.dbook.domain.review.Review
import com.dbook.domain.review.ReviewNotFoundException
import com.dbook.domain.review.ReviewReportRepository
import com.dbook.domain.review.ReviewRepository
import com.dbook.domain.review.toAuditSnapshot
import io.micrometer.observation.annotation.Observed
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/** The moderation queue (`REPORTED`: visible with an open report) and the other views of the reviews. */
@Observed(name = "dbook.usecase")
@Service
class SearchAdminReviewsUseCase(
    private val reader: AdminReviewReader,
) {
    fun execute(
        status: AdminReviewStatus?,
        page: PageQuery,
    ): PageResult<AdminReviewSummary> = reader.search(status, page)
}

data class HideReviewCommand(
    val actor: Actor,
    val reviewId: Long,
    val reason: String,
)

/** Takes a review off the destination (it stops showing and counting) and closes its reports. Audited. */
@Observed(name = "dbook.usecase")
@Service
class HideReviewUseCase(
    private val reviewRepository: ReviewRepository,
    private val reportRepository: ReviewReportRepository,
    private val auditLog: AuditLog,
    private val clock: Clock,
) {
    @Transactional
    fun execute(command: HideReviewCommand): Review {
        val before = find(reviewRepository, command.reviewId)
        val now = LocalDateTime.now(clock)
        val hidden = reviewRepository.save(before.hide(command.reason, command.actor.id, now))
        reportRepository.resolveOpen(command.reviewId, now)
        auditLog.record(
            AuditEvent(
                actor = command.actor,
                action = AuditAction.REVIEW_HIDDEN,
                targetId = command.reviewId.toString(),
                before = before.toAuditSnapshot(),
                after = hidden.toAuditSnapshot(),
                reason = hidden.hiddenReason,
            ),
        )
        return hidden
    }
}

/** Puts a hidden review back. Audited. */
@Observed(name = "dbook.usecase")
@Service
class RestoreReviewUseCase(
    private val reviewRepository: ReviewRepository,
    private val auditLog: AuditLog,
) {
    @Transactional
    fun execute(
        actor: Actor,
        reviewId: Long,
    ): Review {
        val before = find(reviewRepository, reviewId)
        val restored = reviewRepository.save(before.restore())
        auditLog.record(
            AuditEvent(
                actor = actor,
                action = AuditAction.REVIEW_RESTORED,
                targetId = reviewId.toString(),
                before = before.toAuditSnapshot(),
                after = restored.toAuditSnapshot(),
            ),
        )
        return restored
    }
}

/** The team looked at the reports and the review stays: they are closed, and the review leaves the queue. Audited. */
@Observed(name = "dbook.usecase")
@Service
class DismissReviewReportsUseCase(
    private val reviewRepository: ReviewRepository,
    private val reportRepository: ReviewReportRepository,
    private val auditLog: AuditLog,
    private val clock: Clock,
) {
    @Transactional
    fun execute(
        actor: Actor,
        reviewId: Long,
    ): Int {
        val review = find(reviewRepository, reviewId)
        val dismissed = reportRepository.resolveOpen(reviewId, LocalDateTime.now(clock))
        check(dismissed > 0) { "Review $reviewId has no open reports" }
        auditLog.record(
            AuditEvent(
                actor = actor,
                action = AuditAction.REVIEW_REPORTS_DISMISSED,
                targetId = reviewId.toString(),
                before = review.toAuditSnapshot() + ("openReports" to dismissed),
                after = review.toAuditSnapshot() + ("openReports" to 0),
            ),
        )
        return dismissed
    }
}

private fun find(
    reviewRepository: ReviewRepository,
    id: Long,
): Review = reviewRepository.findById(id) ?: throw ReviewNotFoundException(id)
