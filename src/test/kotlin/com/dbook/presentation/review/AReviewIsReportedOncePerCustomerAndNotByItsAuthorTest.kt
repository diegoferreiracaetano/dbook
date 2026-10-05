package com.dbook.presentation.review

import kotlin.test.Test
import kotlin.test.assertEquals

class AReviewIsReportedOncePerCustomerAndNotByItsAuthorTest : ReviewFixture() {
    @Test
    fun `given a review when the same customer reports twice and the author reports their own then both are a 409`() {
        val author = reviewer(newDestination(), 3)
        val (reporter) = reviewer(newDestination(), 5)

        assertEquals(204, reportReview(reporter, author.reviewId).response.status)

        assertEquals(409, reportReview(reporter, author.reviewId, "Again").response.status)
        assertEquals(409, reportReview(author.token, author.reviewId).response.status)
        assertEquals(
            1,
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM review_report WHERE review_id = ?",
                Int::class.java,
                author.reviewId,
            ),
        )
    }

    @Test
    fun `given an unknown review or a missing reason when reporting then it is a 404 or a 400`() {
        val (reporter) = reviewer(newDestination(), 5)
        val author = reviewer(newDestination(), 3)

        assertEquals(404, reportReview(reporter, 999_999_999).response.status)
        assertEquals(400, reportReview(reporter, author.reviewId, " ").response.status)
    }
}
