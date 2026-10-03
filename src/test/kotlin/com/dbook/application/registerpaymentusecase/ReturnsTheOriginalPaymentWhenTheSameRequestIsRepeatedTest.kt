package com.dbook.application.registerpaymentusecase

import kotlin.test.Test
import kotlin.test.assertEquals

class ReturnsTheOriginalPaymentWhenTheSameRequestIsRepeatedTest : RegisterPaymentUseCaseFixture() {
    @Test
    fun `given a payment made with a key when the same request is repeated then it pays only once`() {
        val first = useCase.execute(command())
        val retry = useCase.execute(command())

        assertEquals(first.id, retry.id)
        assertEquals(1, paymentRepository.saved.size)
    }
}
