package com.dbook.application.payment.registerpaymentusecase

import org.springframework.transaction.support.TransactionSynchronizationManager
import kotlin.test.Test
import kotlin.test.assertEquals

class DoesNotCountAPaymentThatNeverCommitsTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given a payment whose transaction never commits then it is not counted as created`() {
        TransactionSynchronizationManager.initSynchronization()
        try {
            useCase.execute(command())
            // afterCommit() is deliberately not invoked: that is what a rollback looks like
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }

        assertEquals(0.0, counted("dbook.payment", "created"))
    }
}
