package com.dbook.application.createreviewusecase

import com.dbook.application.CreateReviewCommand
import com.dbook.domain.NotBookingOwnerException
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
                    comment = null,
                ),
            )
        }
    }
}
