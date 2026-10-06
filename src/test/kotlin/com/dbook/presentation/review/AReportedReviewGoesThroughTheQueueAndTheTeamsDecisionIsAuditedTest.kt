package com.dbook.presentation.review

import com.dbook.domain.common.access.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AReportedReviewGoesThroughTheQueueAndTheTeamsDecisionIsAuditedTest : ReviewFixture() {
    @Test
    fun `given a reported review when the team hides it then it leaves the destination and the queue, audited`() {
        val destination = newDestination()
        val author = reviewer(destination, 1, "Awful")
        val (reporter) = reviewer(newDestination(), 5)
        val staffEmail = uniqueEmail()
        val admin = registerStaffAndLogin(staffEmail, Role.SUPER_ADMIN)

        assertEquals(204, reportReview(reporter, author.reviewId).response.status)

        assertTrue(author.reviewId in queueIds(admin, "REPORTED"))
        val queued = body(adminReviews(admin, "REPORTED"))["items"].first { it["id"].asLong() == author.reviewId }
        assertEquals(1, queued["openReports"].asInt())
        assertEquals("Offensive language", queued["lastReportReason"].asText())
        assertEquals(destination, queued["destination"].asText())

        assertEquals(200, hideReview(admin, author.reviewId).response.status)

        assertEquals(0, body(publicReviews(destination))["summary"]["total"].asInt())
        assertFalse(author.reviewId in queueIds(admin, "REPORTED"))
        assertTrue(author.reviewId in queueIds(admin, "HIDDEN"))
        val entry = auditEntries(admin, "action=REVIEW_HIDDEN&targetId=${author.reviewId}")[0]
        assertEquals(userIdOf(staffEmail), entry["actorId"].asLong())
        assertEquals("VISIBLE", entry["before"]["status"].asText())
        assertEquals("HIDDEN", entry["after"]["status"].asText())
        assertFalse(entry.toString().contains("Awful"))
        assertEquals(404, reportReview(reporter, author.reviewId).response.status)
    }

    @Test
    fun `given a hidden review when restored then it shows again, and a visible one cannot be restored`() {
        val destination = newDestination()
        val author = reviewer(destination, 2)
        val staff = support()
        hideReview(staff, author.reviewId)

        assertEquals(200, moderate(staff, author.reviewId, "restore").response.status)

        assertEquals(1, body(publicReviews(destination))["summary"]["total"].asInt())
        assertEquals(409, moderate(staff, author.reviewId, "restore").response.status)
    }

    @Test
    fun `given a hidden review when hiding it again then it is a 409`() {
        val staff = support()
        val author = reviewer(newDestination(), 2)
        hideReview(staff, author.reviewId)

        assertEquals(409, hideReview(staff, author.reviewId).response.status)
    }

    @Test
    fun `given a review that stays when the reports are dismissed then it leaves the queue but remains visible`() {
        val destination = newDestination()
        val author = reviewer(destination, 4)
        val (reporter) = reviewer(newDestination(), 5)
        val staff = support()
        reportReview(reporter, author.reviewId, "I disagree")

        assertEquals(204, moderate(staff, author.reviewId, "dismiss-reports").response.status)

        assertFalse(author.reviewId in queueIds(staff, "REPORTED"))
        assertEquals(1, body(publicReviews(destination))["summary"]["total"].asInt())
        assertEquals(409, moderate(staff, author.reviewId, "dismiss-reports").response.status)
    }

    @Test
    fun `given a short reason when hiding then it is a 400 and the review stays visible`() {
        val destination = newDestination()
        val author = reviewer(destination, 4)

        assertEquals(400, hideReview(support(), author.reviewId, "short").response.status)

        assertEquals(1, body(publicReviews(destination))["summary"]["total"].asInt())
    }
}
