package com.dbook.domain.review

import kotlin.test.Test
import kotlin.test.assertEquals

class CreatesAValidReviewTest {
    @Test
    fun `given valid data when a Review is built then its fields are set`() {
        val review =
            Review(
                bookingId = 100,
                customerId = 1,
                rating = 5,
                comment = "Great flight!",
            )

        assertEquals(100, review.bookingId)
        assertEquals(5, review.rating)
        assertEquals("Great flight!", review.comment)
    }
}
