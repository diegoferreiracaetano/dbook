package com.dbook.application.review

import com.dbook.application.audit.FakeAuditLog
import com.dbook.application.booking.cancelbookingusecase.FakeBookingRepository
import com.dbook.application.review.createreviewusecase.FakeReviewRepository
import com.dbook.domain.review.Review
import com.dbook.domain.review.ReviewReport
import com.dbook.domain.review.ReviewReportRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class InMemoryReviewReports : ReviewReportRepository {
    val reports = mutableListOf<ReviewReport>()
    val resolved = mutableSetOf<Long>()

    override fun add(report: ReviewReport): Boolean {
        if (reports.any { it.reviewId == report.reviewId && it.reporterId == report.reporterId }) return false
        reports += report
        return true
    }

    override fun resolveOpen(
        reviewId: Long,
        at: LocalDateTime,
    ): Int {
        val open = reports.count { it.reviewId == reviewId && reviewId !in resolved }
        resolved += reviewId
        return open
    }
}

// Review 1 is customer 7's, visible, 4 stars. Review 2 is customer 8's and hidden. The clock is 2026-10-04 12:00.
abstract class ReviewUseCaseFixture {
    protected val now: Instant = Instant.parse("2026-10-04T12:00:00Z")
    protected val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    protected val author = 7L
    protected val stranger = 9L

    protected val visible = Review(1, bookingId = 100, customerId = author, rating = 4, comment = "Good trip")
    protected val hidden =
        Review(
            2,
            bookingId = 101,
            customerId = 8,
            rating = 1,
            comment = "Awful",
        ).hide("Offensive language", 99, LocalDateTime.MIN)
    protected val reviews = FakeReviewRepository(listOf(visible, hidden))
    protected val reports = InMemoryReviewReports()
    protected val audit = FakeAuditLog()
    protected val bookings = FakeBookingRepository(emptyList())
}
