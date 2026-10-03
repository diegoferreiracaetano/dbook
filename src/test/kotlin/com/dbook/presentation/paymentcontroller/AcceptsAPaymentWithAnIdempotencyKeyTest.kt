package com.dbook.presentation.paymentcontroller

import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post

class AcceptsAPaymentWithAnIdempotencyKeyTest : PaymentControllerFixture() {
    @Test
    fun `given an Idempotency-Key header when posting a payment then it returns 201 with that key sent on`() {
        // the stub only matches a command carrying this exact key: any other value would
        // leave the mock returning null and the request would not come back as 201
        given(registerPaymentUseCase.execute(expectedCommand)).willReturn(payment)

        mockMvc.post("/payments") {
            principal = authentication
            header("Idempotency-Key", idempotencyKey)
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { value(7) }
            jsonPath("$.bookingIds[0]") { value(100) }
        }
    }
}
