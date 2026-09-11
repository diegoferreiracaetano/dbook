package com.dbook.application.registerbookingusecase

import com.dbook.domain.SeatNotFoundException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThrowsWhenSeatDoesNotExistTest : RegisterBookingUseCaseFixture() {
    @Test
    fun `given a nonexistent seatId when registering a booking then it throws SeatNotFoundException`() {
        assertFailsWith<SeatNotFoundException> {
            useCase.execute(command(seatIdOverride = 999))
        }
    }
}
