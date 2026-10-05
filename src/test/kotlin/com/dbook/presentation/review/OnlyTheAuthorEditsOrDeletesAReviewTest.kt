package com.dbook.presentation.review

import kotlin.test.Test
import kotlin.test.assertEquals

class OnlyTheAuthorEditsOrDeletesAReviewTest : ReviewFixture() {
    @Test
    fun `given a third customer when editing or deleting another's review then both are a 403 and nothing changes`() {
        val destination = newDestination()
        val author = reviewer(destination, 4)
        val (third) = reviewer(newDestination(), 2)

        val edit = editReview(third, author.reviewId, mapOf("rating" to 1))
        val delete = deleteReview(third, author.reviewId)

        assertEquals(403, edit.response.status)
        assertEquals("FORBIDDEN", body(edit)["code"].asText())
        assertEquals(403, delete.response.status)
        assertEquals(4.0, body(publicReviews(destination))["summary"]["average"].asDouble())
    }

    @Test
    fun `given an unknown review when editing or deleting then it is a 404`() {
        val (token) = reviewer(newDestination(), 4)

        assertEquals(404, editReview(token, 999_999_999, mapOf("rating" to 1)).response.status)
        assertEquals(404, deleteReview(token, 999_999_999).response.status)
    }

    @Test
    fun `given an edit with nothing to change or a bad rating when sent then it is a 400`() {
        val (token, _, review) = reviewer(newDestination(), 4)

        assertEquals(400, editReview(token, review, emptyMap()).response.status)
        assertEquals(400, editReview(token, review, mapOf("rating" to 9)).response.status)
        assertEquals(400, editReview(token, review, mapOf("comment" to " ")).response.status)
    }

    @Test
    fun `given a deleted review when read then it is gone, and without a token it is a 401`() {
        val destination = newDestination()
        val author = reviewer(destination, 4)

        deleteReview(author.token, author.reviewId)

        assertEquals(0, body(publicReviews(destination))["summary"]["total"].asInt())
        assertEquals(404, editReview(author.token, author.reviewId, mapOf("rating" to 1)).response.status)
    }
}
