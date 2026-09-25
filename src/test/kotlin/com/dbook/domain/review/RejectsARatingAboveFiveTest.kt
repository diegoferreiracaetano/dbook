package com.dbook.domain.review

import com.dbook.domain.Review
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsARatingAboveFiveTest {
    @Test
    fun `given a rating of 6 when a Review is built then it throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            Review(bookingId = 100, customerId = 1, rating = 6, comment = null)
        }
    }
}
