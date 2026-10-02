package com.dbook.application.listmybookingsusecase

import kotlin.test.Test
import kotlin.test.assertNull

class LeavesTheReviewEmptyForAnUnreviewedBookingTest : ListMyBookingsUseCaseFixture() {
    @Test
    fun `given a booking nobody reviewed when listed then its review is null`() {
        val result = useCase(bookings = listOf(ownBooking), seats = listOf(ownSeat)).execute(ownerId)

        assertNull(result.single().review)
    }
}
