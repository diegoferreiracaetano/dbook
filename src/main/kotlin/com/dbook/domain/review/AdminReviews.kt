package com.dbook.domain.review

import com.dbook.domain.common.PageQuery
import com.dbook.domain.common.PageResult
import java.time.LocalDateTime

/** REPORTED: visible and with an open report (the moderation queue). HIDDEN: taken down by the team. */
enum class AdminReviewStatus { REPORTED, HIDDEN, VISIBLE }

data class AdminReviewSummary(
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
)

interface AdminReviewReader {
    fun search(
        status: AdminReviewStatus?,
        page: PageQuery,
    ): PageResult<AdminReviewSummary>
}
