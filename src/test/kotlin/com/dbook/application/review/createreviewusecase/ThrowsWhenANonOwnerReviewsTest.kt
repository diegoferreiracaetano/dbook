package com.dbook.application.review.createreviewusecase

import com.dbook.application.review.CreateReviewCommand
import com.dbook.domain.booking.NotBookingOwnerException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenANonOwnerReviewsTest : CreateReviewUseCaseFixture() {
    @Test
    fun `given another user's booking when a non-owner reviews it then it throws NotBookingOwnerException`() {
        assertFailsWith<NotBookingOwnerException> {
            useCase.execute(
                CreateReviewCommand(
                    bookingId = confirmedBookingId,
                    customerId = 999,
                    rating = 5,
                    comment = "Great flight!",
                ),
            )
        }
    }
}
