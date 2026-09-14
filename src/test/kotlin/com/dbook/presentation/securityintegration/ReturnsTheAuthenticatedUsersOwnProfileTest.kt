package com.dbook.presentation.securityintegration

import org.springframework.test.web.servlet.get
import kotlin.test.Test

class ReturnsTheAuthenticatedUsersOwnProfileTest : SecurityIntegrationFixture() {
    @Test
    fun `given a valid token when getting the own profile then it returns the caller's own data`() {
        val email = uniqueEmail()
        val token = registerAndLogin(email, name = "Diego Ferreira")

        mockMvc.get("/users/me") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value(email) }
            jsonPath("$.name") { value("Diego Ferreira") }
            jsonPath("$.role") { value("CLIENT") }
        }
    }
}
