package com.dbook.application.registerbookingusecase

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenSeatDoesNotBelongToBookableTest : RegisterBookingUseCaseFixture() {
    @Test
    fun `given a mismatched seat when registering a booking then it throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            useCase.execute(command(seatIdOverride = otherBookableSeatId))
        }
    }
}
