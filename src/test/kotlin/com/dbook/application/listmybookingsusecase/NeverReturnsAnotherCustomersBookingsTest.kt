package com.dbook.application.listmybookingsusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class NeverReturnsAnotherCustomersBookingsTest : ListMyBookingsUseCaseFixture() {
    @Test
    fun `given bookings belonging to another customer when listing then they are never included`() {
        val useCase =
            useCase(
                bookings = listOf(ownBooking, otherOwnerBooking),
                seats = listOf(ownSeat, otherOwnerSeat),
            )

        val result = useCase.execute(ownerId)

        assertEquals(1, result.size)
        assertEquals(ownBooking, result.single().booking)
    }
}
