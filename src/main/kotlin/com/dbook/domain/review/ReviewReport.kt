package com.dbook.domain.review

import java.time.LocalDateTime

private const val MIN_REASON_LENGTH = 3
private const val MAX_REASON_LENGTH = 500

/** A customer's complaint about a review. A customer can report a review once. */
data class ReviewReport(
    val reviewId: Long,
    val reporterId: Long,
    val reason: String,
    val createdAt: LocalDateTime,
) {
    init {
        require(reason.trim().length in MIN_REASON_LENGTH..MAX_REASON_LENGTH) {
            "reason must have between $MIN_REASON_LENGTH and $MAX_REASON_LENGTH characters"
        }
    }
}

interface ReviewReportRepository {
    /** @return false when this customer had already reported this review (nothing is stored then). */
    fun add(report: ReviewReport): Boolean

    /** The open reports of the review are closed: the team acted on it. @return how many were open. */
    fun resolveOpen(
        reviewId: Long,
        at: LocalDateTime,
    ): Int
}
