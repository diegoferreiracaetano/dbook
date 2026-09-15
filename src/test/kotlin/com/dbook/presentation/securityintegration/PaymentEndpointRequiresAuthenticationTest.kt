package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test

class PaymentEndpointRequiresAuthenticationTest : SecurityIntegrationFixture() {
    @Test
    fun `given no token when posting a payment then it returns 401`() {
        mockMvc.post("/payments") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect { status { isUnauthorized() } }
    }
}
