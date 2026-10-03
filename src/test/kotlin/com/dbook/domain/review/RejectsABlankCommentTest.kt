package com.dbook.domain.review

import kotlin.test.Test
import kotlin.test.assertFailsWith

class RejectsABlankCommentTest {
    @Test
    fun `given a blank comment when a Review is built then it throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            Review(bookingId = 100, customerId = 1, rating = 5, comment = "   ")
        }
    }
}
