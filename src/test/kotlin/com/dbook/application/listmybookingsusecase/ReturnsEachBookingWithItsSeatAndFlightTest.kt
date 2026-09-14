package com.dbook.application.listmybookingsusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class ReturnsEachBookingWithItsSeatAndFlightTest : ListMyBookingsUseCaseFixture() {
    @Test
    fun `given several bookings when listing then each comes back with its own seat and flight`() {
        val useCase = useCase(bookings = listOf(ownBooking), seats = listOf(ownSeat))

        val result = useCase.execute(ownerId)

        assertEquals(1, result.size)
        assertEquals(ownBooking, result.single().booking)
        assertEquals(ownSeat, result.single().seat)
        assertEquals(flight, result.single().booking.bookable)
    }
}
