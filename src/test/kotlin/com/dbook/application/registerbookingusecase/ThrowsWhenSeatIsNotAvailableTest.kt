package com.dbook.application.registerbookingusecase

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenSeatIsNotAvailableTest : RegisterBookingUseCaseFixture() {
    @Test
    fun `given a seat that is already RESERVED when registering a booking then it throws IllegalStateException`() {
        seatRepository.reserve(seatId)

        assertFailsWith<IllegalStateException> {
            useCase.execute(command())
        }
    }
}
