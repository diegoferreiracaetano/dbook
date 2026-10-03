package com.dbook.application.payment.registerpaymentusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class CountsARepeatedRequestAsReplayedTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given a payment already made when the same request is repeated then it counts as replayed`() {
        executeCommitted(command())
        executeCommitted(command())

        assertEquals(1.0, counted("dbook.payment", "created"))
        assertEquals(1.0, counted("dbook.payment", "replayed"))
    }
}
