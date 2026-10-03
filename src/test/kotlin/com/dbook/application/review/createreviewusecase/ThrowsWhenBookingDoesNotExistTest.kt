package com.dbook.application.review.createreviewusecase

import com.dbook.application.review.CreateReviewCommand
import com.dbook.domain.booking.BookingNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenBookingDoesNotExistTest : CreateReviewUseCaseFixture() {
    @Test
    fun `given a booking id that doesn't exist when reviewed then it throws BookingNotFoundException`() {
        assertFailsWith<BookingNotFoundException> {
            useCase.execute(
                CreateReviewCommand(
                    bookingId = 999_999,
                    customerId = ownerId,
                    rating = 5,
                    comment = "Great flight!",
                ),
            )
        }
    }
}
