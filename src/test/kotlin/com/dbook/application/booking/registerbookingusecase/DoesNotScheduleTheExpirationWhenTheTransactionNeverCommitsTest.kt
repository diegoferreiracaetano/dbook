package com.dbook.application.booking.registerbookingusecase

import kotlin.test.Test
import kotlin.test.assertTrue

class DoesNotScheduleTheExpirationWhenTheTransactionNeverCommitsTest : RegisterBookingUseCaseFixture() {
    @Test
    fun `given a booking whose transaction never commits then no expiration is scheduled`() {
        withTransactionSynchronization {
            useCase.execute(command())
            // afterCommit() is deliberately not invoked: that is what a rollback looks like
        }

        assertTrue(bookingExpirationScheduler.scheduled.isEmpty())
    }
}
