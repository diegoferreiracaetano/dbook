package com.dbook.application.booking.registerbookingusecase

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NoEventIsAddedForABookingThatFailsTest : RegisterBookingUseCaseFixture() {
    @Test
    fun `given a seat that does not exist when booking then it fails and no expiration event is added`() {
        withTransactionSynchronization {
            assertFailsWith<RuntimeException> { useCase.execute(command(seatIdOverride = 999)) }
        }

        assertTrue(outbox.events.isEmpty())
    }
}
