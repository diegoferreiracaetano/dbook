package com.dbook.presentation.paymentcontroller

import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post

class ReturnsBadRequestWhenTheIdempotencyKeyIsMissingTest : PaymentControllerFixture() {
    @Test
    fun `given no Idempotency-Key header when posting a payment then it returns 400 without paying`() {
        mockMvc.post("/payments") {
            principal = authentication
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("Missing request header: Idempotency-Key") }
        }

        verifyNoInteractions(registerPaymentUseCase)
    }
}
