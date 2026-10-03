package com.dbook.application.payment.registerpaymentusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class ReturnsTheOriginalPaymentWhenTheSameRequestIsRepeatedTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given a payment made with a key when the same request is repeated then it pays only once`() {
        val first = executeCommitted(command())
        val retry = executeCommitted(command())

        assertEquals(first.id, retry.id)
        assertEquals(1, paymentRepository.saved.size)
    }
}
