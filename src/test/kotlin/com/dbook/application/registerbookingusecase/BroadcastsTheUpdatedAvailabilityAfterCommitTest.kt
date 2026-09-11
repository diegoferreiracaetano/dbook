package com.dbook.application.registerbookingusecase

import org.springframework.transaction.support.TransactionSynchronizationManager
import kotlin.test.Test
import kotlin.test.assertEquals

class BroadcastsTheUpdatedAvailabilityAfterCommitTest : RegisterBookingUseCaseFixture() {
    @Test
    fun `given a successful booking when the transaction commits then it broadcasts the remaining availability`() {
        withTransactionSynchronization {
            useCase.execute(command())
            TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
        }

        assertEquals(bookableId, availabilityBroadcaster.lastBookableId)
        assertEquals(0, availabilityBroadcaster.lastAvailableCapacity)
    }
}
