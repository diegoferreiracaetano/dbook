package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test

class BookingEndpointsRequireAuthenticationTest : SecurityIntegrationFixture() {
    @Test
    fun `given no token when posting a booking then it returns 401`() {
        mockMvc.post("/bookings") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect { status { isUnauthorized() } }
    }
}
