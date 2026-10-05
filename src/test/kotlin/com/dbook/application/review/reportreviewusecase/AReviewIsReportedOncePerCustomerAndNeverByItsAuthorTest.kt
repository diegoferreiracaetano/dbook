package com.dbook.application.review.reportreviewusecase

import com.dbook.application.review.ReportReviewCommand
import com.dbook.application.review.ReportReviewUseCase
import com.dbook.application.review.ReviewUseCaseFixture
import com.dbook.domain.review.ReviewNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AReviewIsReportedOncePerCustomerAndNeverByItsAuthorTest : ReviewUseCaseFixture() {
    private val useCase = ReportReviewUseCase(reviews, reports, clock)

    @Test
    fun `given a visible review when another customer reports it then the report is stored`() {
        useCase.execute(ReportReviewCommand(1, stranger, "Offensive language"))

        assertEquals(listOf(stranger), reports.reports.map { it.reporterId })
    }

    @Test
    fun `given a report already made when the same customer reports again then it is a conflict`() {
        useCase.execute(ReportReviewCommand(1, stranger, "Offensive language"))

        assertFailsWith<IllegalStateException> { useCase.execute(ReportReviewCommand(1, stranger, "Again")) }

        assertEquals(1, reports.reports.size)
    }

    @Test
    fun `given the author when reporting their own review then it is refused`() {
        assertFailsWith<IllegalStateException> { useCase.execute(ReportReviewCommand(1, author, "Oops")) }
    }

    @Test
    fun `given a hidden review when reporting it then it is not found, as if it did not exist`() {
        assertFailsWith<ReviewNotFoundException> { useCase.execute(ReportReviewCommand(2, stranger, "Awful")) }
    }

    @Test
    fun `given a blank or huge reason when reporting then it is refused`() {
        assertFailsWith<IllegalArgumentException> { useCase.execute(ReportReviewCommand(1, stranger, " ")) }
        assertFailsWith<IllegalArgumentException> { useCase.execute(ReportReviewCommand(1, stranger, "x".repeat(501))) }
    }
}
