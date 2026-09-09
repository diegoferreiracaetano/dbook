package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test

class AdminEndpointRejectsAClientTokenTest : SecurityIntegrationFixture() {
    @Test
    fun `given a CLIENT token when posting to admin flights then it returns 403`() {
        val token = registerAndLogin(uniqueEmail())

        // A well-formed body on purpose: @PreAuthorize is method-level AOP, which only
        // runs once Spring MVC has already bound the request body. A malformed body
        // (e.g. "{}") would 400 before authorization is even checked, hiding the thing
        // this test is actually about.
        mockMvc.post("/admin/flights") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = validFlightRequestBody()
        }.andExpect { status { isForbidden() } }
    }
}
