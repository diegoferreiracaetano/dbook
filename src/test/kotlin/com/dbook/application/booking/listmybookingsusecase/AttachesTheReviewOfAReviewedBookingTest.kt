package com.dbook.application.booking.listmybookingsusecase

import com.dbook.domain.review.Review
import kotlin.test.Test
import kotlin.test.assertEquals

class AttachesTheReviewOfAReviewedBookingTest : ListMyBookingsUseCaseFixture() {
    @Test
    fun `given a booking that was reviewed when listed then its details carry the review`() {
        val review = Review(id = 1, bookingId = bookingId, customerId = ownerId, rating = 5, comment = "Great flight!")

        val result =
            useCase(
                bookings = listOf(ownBooking),
                seats = listOf(ownSeat),
                reviews = listOf(review),
            ).execute(ownerId)

        assertEquals(review, result.single().review)
    }
}
