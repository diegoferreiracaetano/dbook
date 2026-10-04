package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import kotlin.test.Test

class UpdatesTheAuthenticatedUsersNameTest : SecurityIntegrationFixture() {
    @Test
    fun `given a new name when updating the own profile then it persists and is returned`() {
        val email = uniqueEmail()
        val token = registerAndLogin(email, name = "Old Name")

        mockMvc.patch("/v1/users/me") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("name" to "New Name"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("New Name") }
        }

        mockMvc.get("/v1/users/me") {
            header("Authorization", "Bearer $token")
        }.andExpect { jsonPath("$.name") { value("New Name") } }
    }
}
