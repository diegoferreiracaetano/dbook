package com.dbook.infrastructure.persistence.review

import com.dbook.domain.review.ReviewReport
import com.dbook.domain.review.ReviewReportRepository
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class ReviewReportRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : ReviewReportRepository {
    // the unique (review, reporter) decides a second report from the same customer, even two at the same time
    @Transactional
    override fun add(report: ReviewReport): Boolean =
        jdbc.update(
            "INSERT INTO review_report (review_id, reporter_id, reason, created_at) " +
                "VALUES (:review, :reporter, :reason, :now) ON CONFLICT (review_id, reporter_id) DO NOTHING",
            mapOf(
                "review" to report.reviewId, "reporter" to report.reporterId, "reason" to report.reason.trim(),
                "now" to Timestamp.valueOf(report.createdAt),
            ),
        ) == 1

    @Transactional
    override fun resolveOpen(
        reviewId: Long,
        at: LocalDateTime,
    ): Int =
        jdbc.update(
            "UPDATE review_report SET resolved_at = :now WHERE review_id = :review AND resolved_at IS NULL",
            mapOf("review" to reviewId, "now" to Timestamp.valueOf(at)),
        )
}
