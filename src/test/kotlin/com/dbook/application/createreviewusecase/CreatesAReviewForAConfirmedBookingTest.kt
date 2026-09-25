package com.dbook.application.createreviewusecase

import com.dbook.application.CreateReviewCommand
import kotlin.test.Test
import kotlin.test.assertEquals

class CreatesAReviewForAConfirmedBookingTest : CreateReviewUseCaseFixture() {
    @Test
    fun `given a CONFIRMED booking when reviewed then the review is saved with its rating and comment`() {
        val review =
            useCase.execute(
                CreateReviewCommand(
                    bookingId = confirmedBookingId,
                    customerId = ownerId,
                    rating = 5,
                    comment = "Great flight!",
                ),
            )

        assertEquals(5, review.rating)
        assertEquals("Great flight!", review.comment)
        assertEquals(confirmedBookingId, review.bookingId)
    }
}
