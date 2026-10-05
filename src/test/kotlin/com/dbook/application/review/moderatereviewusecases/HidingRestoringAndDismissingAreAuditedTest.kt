package com.dbook.application.review.moderatereviewusecases

import com.dbook.application.review.DismissReviewReportsUseCase
import com.dbook.application.review.HideReviewCommand
import com.dbook.application.review.HideReviewUseCase
import com.dbook.application.review.ReportReviewCommand
import com.dbook.application.review.ReportReviewUseCase
import com.dbook.application.review.RestoreReviewUseCase
import com.dbook.application.review.ReviewUseCaseFixture
import com.dbook.domain.audit.AuditAction
import com.dbook.domain.identity.Actor
import com.dbook.domain.identity.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HidingRestoringAndDismissingAreAuditedTest : ReviewUseCaseFixture() {
    private val staff = Actor(99, Role.SUPPORT)
    private val hide = HideReviewUseCase(reviews, reports, audit, clock)
    private val restore = RestoreReviewUseCase(reviews, audit)
    private val dismiss = DismissReviewReportsUseCase(reviews, reports, audit, clock)
    private val report = ReportReviewUseCase(reviews, reports, clock)

    @Test
    fun `given a reported review when hiding it then who and why are kept, and the audit has both states`() {
        report.execute(ReportReviewCommand(1, stranger, "Offensive language"))

        val result = hide.execute(HideReviewCommand(staff, 1, "Offensive language towards the crew"))

        assertFalse(result.isVisible)
        assertEquals(99L, reviews.findById(1)?.hiddenBy)
        val event = audit.events.single()
        assertEquals(AuditAction.REVIEW_HIDDEN, event.action)
        assertEquals("VISIBLE", event.before?.get("status"))
        assertEquals("HIDDEN", event.after?.get("status"))
        assertEquals("Offensive language towards the crew", event.reason)
        assertTrue(1L in reports.resolved)
    }

    @Test
    fun `given a hidden review when hiding it again or giving a short reason then it is refused`() {
        assertFailsWith<IllegalStateException> { hide.execute(HideReviewCommand(staff, 2, "Offensive language")) }
        assertFailsWith<IllegalArgumentException> { hide.execute(HideReviewCommand(staff, 1, "short")) }
    }

    @Test
    fun `given a hidden review when restoring it then it shows again, and a visible one cannot be restored`() {
        val result = restore.execute(staff, 2)

        assertTrue(result.isVisible)
        assertEquals(null, result.hiddenReason)
        assertEquals(AuditAction.REVIEW_RESTORED, audit.events.single().action)
        assertFailsWith<IllegalStateException> { restore.execute(staff, 1) }
    }

    @Test
    fun `given open reports when dismissing then they close, and without any open report it is a conflict`() {
        report.execute(ReportReviewCommand(1, stranger, "Not true"))

        assertEquals(1, dismiss.execute(staff, 1))
        assertEquals(AuditAction.REVIEW_REPORTS_DISMISSED, audit.events.single().action)
    }
}
