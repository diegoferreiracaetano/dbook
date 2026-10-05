package com.dbook.presentation.review

import kotlin.test.Test
import kotlin.test.assertEquals

class PagingAndSortingAreStableTest : ReviewFixture() {
    private fun pages(
        destination: String,
        sort: String,
    ): List<List<Long>> =
        (0..2).map { page ->
            body(publicReviews(destination, "sort" to sort, "page" to "$page", "size" to "2"))["reviews"]["items"]
                .map { it["id"].asLong() }
        }

    @Test
    fun `given five reviews when paging by two then every review shows once, in the order asked`() {
        val destination = newDestination()
        val ids = listOf(5, 3, 5, 1, 3).map { reviewer(destination, it).reviewId }

        val byRecent = pages(destination, "RECENT")
        val byRating = pages(destination, "RATING")

        assertEquals(ids.reversed(), byRecent.flatten())
        assertEquals(listOf(ids[2], ids[0], ids[4], ids[1], ids[3]), byRating.flatten())
        assertEquals(listOf(2, 2, 1), byRating.map { it.size })
        val first = body(publicReviews(destination, "size" to "2"))["reviews"]
        assertEquals(5, first["totalElements"].asInt())
        assertEquals(3, first["totalPages"].asInt())
    }
}
