package com.dbook.application.payment.registerpaymentusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class CountsANewPaymentAsCreatedAfterCommitTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given a new payment when it commits then it is counted once as created and never as replayed`() {
        executeCommitted(command())

        assertEquals(1.0, counted("dbook.payment", "created"))
        assertEquals(0.0, counted("dbook.payment", "replayed"))
    }
}
