package com.dbook.presentation.review

import com.dbook.domain.review.AdminReviewSummary
import com.dbook.domain.review.ReviewStatus
import java.time.LocalDateTime

data class AdminReviewResponse(
    val id: Long,
    val bookingId: Long,
    val customerId: Long,
    val customerName: String,
    val destination: String,
    val rating: Int,
    val comment: String,
    val status: ReviewStatus,
    val hiddenReason: String?,
    val openReports: Int,
    val lastReportReason: String?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(review: AdminReviewSummary) =
            AdminReviewResponse(
                review.id, review.bookingId, review.customerId, review.customerName, review.destination,
                review.rating, review.comment, review.status, review.hiddenReason, review.openReports,
                review.lastReportReason, review.createdAt,
            )
    }
}
