package com.dbook.presentation.customerhistory

import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewsShowTheRatingAndTheCommentTest : CustomerHistoryFixture() {
    @Test
    fun `given two reviews when listing then newest first with rating, comment and booking`() {
        val email = uniqueEmail()
        val token = registerAndLogin(email)
        val (first, second) = bookSeats(token, 2)
        pay(token, first)
        pay(token, second)
        review(token, first, rating = 5)
        review(token, second, rating = 2)

        val items = bodyOf(history(supportToken(), userIdOf(email), "reviews"))["items"]

        assertEquals(listOf(2, 5), items.map { it["rating"].asInt() })
        assertEquals(listOf(second, first), items.map { it["bookingId"].asLong() })
        assertEquals("ok", items[0]["comment"].asText())
    }
}
