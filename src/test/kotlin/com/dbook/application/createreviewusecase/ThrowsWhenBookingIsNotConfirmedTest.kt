package com.dbook.application.createreviewusecase

import com.dbook.application.CreateReviewCommand
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenBookingIsNotConfirmedTest : CreateReviewUseCaseFixture() {
    @Test
    fun `given a still-PENDING booking when reviewed then it throws IllegalStateException`() {
        assertFailsWith<IllegalStateException> {
            useCase.execute(
                CreateReviewCommand(
                    bookingId = pendingBookingId,
                    customerId = ownerId,
                    rating = 5,
                    comment = null,
                ),
            )
        }
    }
}
