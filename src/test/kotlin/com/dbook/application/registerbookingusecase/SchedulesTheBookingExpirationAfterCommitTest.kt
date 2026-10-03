package com.dbook.application.registerbookingusecase

import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class SchedulesTheBookingExpirationAfterCommitTest : RegisterBookingUseCaseFixture() {
    @Test
    fun `given a successful booking when the transaction commits then it schedules its expiration in 15 minutes`() {
        lateinit var bookingId: Number
        withTransactionSynchronization {
            bookingId = requireNotNull(useCase.execute(command()).id)
            TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
        }

        assertEquals(listOf(bookingId.toLong() to Duration.ofMinutes(15)), bookingExpirationScheduler.scheduled)
    }
}
