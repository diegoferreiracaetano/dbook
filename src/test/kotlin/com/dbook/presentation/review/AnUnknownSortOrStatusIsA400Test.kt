package com.dbook.presentation.review

import kotlin.test.Test
import kotlin.test.assertEquals

class AnUnknownSortOrStatusIsA400Test : ReviewFixture() {
    @Test
    fun `given an unknown sort or status when listing reviews then it is a 400 with its code, not a 401`() {
        val destination = newDestination()

        val sort = publicReviews(destination, "sort" to "NOPE")
        val status = adminReviews(support(), "NOPE")

        assertEquals(400, sort.response.status)
        assertEquals("VALIDATION_FAILED", body(sort)["code"].asText())
        assertEquals(400, status.response.status)
    }
}
