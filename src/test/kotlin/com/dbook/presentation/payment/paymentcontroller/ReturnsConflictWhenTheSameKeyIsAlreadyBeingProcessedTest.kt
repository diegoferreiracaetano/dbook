package com.dbook.presentation.payment.paymentcontroller

import com.dbook.domain.payment.DuplicateIdempotencyKeyException
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post

class ReturnsConflictWhenTheSameKeyIsAlreadyBeingProcessedTest : PaymentControllerFixture() {
    @Test
    fun `given a request with the same key still being processed when posting a payment then it returns 409`() {
        given(registerPaymentUseCase.execute(expectedCommand))
            .willThrow(DuplicateIdempotencyKeyException(IllegalStateException("unique index violated")))

        mockMvc.post("/v1/payments") {
            principal = authentication
            header("Idempotency-Key", idempotencyKey)
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
        }
    }
}
