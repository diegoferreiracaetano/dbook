package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.Test

class AdminEndpointRejectsRequestsWithoutATokenTest : SecurityIntegrationFixture() {
    @Test
    fun `given no token when posting to admin flights then it returns 401`() {
        mockMvc.post("/admin/flights") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect { status { isUnauthorized() } }
    }
}
