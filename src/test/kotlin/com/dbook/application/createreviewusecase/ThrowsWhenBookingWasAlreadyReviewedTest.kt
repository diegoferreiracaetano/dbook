package com.dbook.application.createreviewusecase

import com.dbook.application.CreateReviewCommand
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenBookingWasAlreadyReviewedTest : CreateReviewUseCaseFixture() {
    @Test
    fun `given a booking that was already reviewed when reviewed again then it throws IllegalStateException`() {
        useCase.execute(
            CreateReviewCommand(
                bookingId = confirmedBookingId,
                customerId = ownerId,
                rating = 5,
                comment = null,
            ),
        )

        assertFailsWith<IllegalStateException> {
            useCase.execute(
                CreateReviewCommand(
                    bookingId = confirmedBookingId,
                    customerId = ownerId,
                    rating = 1,
                    comment = "Actually, it was late",
                ),
            )
        }
    }
}
