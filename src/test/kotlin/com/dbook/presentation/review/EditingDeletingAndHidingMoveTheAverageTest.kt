package com.dbook.presentation.review

import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

class EditingDeletingAndHidingMoveTheAverageTest : ReviewFixture() {
    private fun average(destination: String): Double? =
        body(publicReviews(destination))["summary"]["average"].takeIf { !it.isNull }?.asDouble()

    private fun featuredAverage(destination: String): Double? =
        body(mockMvc.get("/v1/destinations").andReturn())
            .first { it["iataCode"].asText() == destination }["averageRating"].takeIf { !it.isNull }?.asDouble()

    @Test
    fun `given two reviews when one is edited, hidden, restored and deleted then the average follows each step`() {
        val destination = newDestination()
        val high = reviewer(destination, 5)
        val low = reviewer(destination, 1)
        assertEquals(3.0, average(destination))

        val edited = editReview(low.token, low.reviewId, mapOf("rating" to 3, "comment" to "Better than I said"))
        assertEquals(200, edited.response.status)
        assertEquals(4.0, average(destination))
        assertEquals(true, body(publicReviews(destination))["reviews"]["items"].any { it["edited"].asBoolean() })

        val staff = support()
        hideReview(staff, high.reviewId)
        assertEquals(3.0, average(destination))
        assertEquals(3.0, featuredAverage(destination))

        moderate(staff, high.reviewId, "restore")
        assertEquals(4.0, average(destination))

        assertEquals(204, deleteReview(high.token, high.reviewId).response.status)
        assertEquals(3.0, average(destination))
        assertEquals(1, body(publicReviews(destination))["summary"]["total"].asInt())
    }
}
