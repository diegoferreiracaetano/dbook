package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import kotlin.test.Test

class UsersMeEndpointsRequireAuthenticationTest : SecurityIntegrationFixture() {
    @Test
    fun `given no token when getting the own profile then it returns 401`() {
        mockMvc.get("/users/me").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `given no token when updating the own name then it returns 401`() {
        mockMvc.patch("/users/me") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("name" to "New Name"))
        }.andExpect { status { isUnauthorized() } }
    }
}
