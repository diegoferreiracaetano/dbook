package com.dbook.presentation.securityintegration

import org.springframework.test.web.servlet.get
import kotlin.test.Test

class AdminMeRejectsAClientTest : SecurityIntegrationFixture() {
    @Test
    fun `given a CLIENT token when asking for the staff profile then it returns 403`() {
        val token = registerAndLogin(uniqueEmail())

        mockMvc.get("/v1/admin/auth/me") { header("Authorization", "Bearer $token") }
            .andExpect { status { isForbidden() } }
    }
}
