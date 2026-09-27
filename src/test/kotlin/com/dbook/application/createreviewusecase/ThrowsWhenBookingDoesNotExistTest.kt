package com.dbook.application.createreviewusecase

import com.dbook.application.CreateReviewCommand
import com.dbook.domain.BookingNotFoundException
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
