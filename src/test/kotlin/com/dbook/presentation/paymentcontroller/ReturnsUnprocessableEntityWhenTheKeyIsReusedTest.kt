package com.dbook.presentation.paymentcontroller

import com.dbook.domain.IdempotencyKeyReusedException
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post

class ReturnsUnprocessableEntityWhenTheKeyIsReusedTest : PaymentControllerFixture() {
    @Test
    fun `given a key already used for another request when posting a payment then it returns 422`() {
        given(registerPaymentUseCase.execute(expectedCommand)).willThrow(IdempotencyKeyReusedException())

        mockMvc.post("/payments") {
            principal = authentication
            header("Idempotency-Key", idempotencyKey)
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isUnprocessableEntity() }
            jsonPath("$.error") { value("Idempotency-Key was already used with a different request") }
        }
    }
}
