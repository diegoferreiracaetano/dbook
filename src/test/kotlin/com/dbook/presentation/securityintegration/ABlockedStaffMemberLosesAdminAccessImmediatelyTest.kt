package com.dbook.presentation.securityintegration

import org.springframework.test.web.servlet.get
import kotlin.test.Test

class ABlockedStaffMemberLosesAdminAccessImmediatelyTest : SecurityIntegrationFixture() {
    @Test
    fun `given a staff token that works when the account is blocked then the very next admin call is 401`() {
        val email = uniqueEmail()
        val token = registerStaffAndLogin(email)
        mockMvc.get("/v1/admin/auth/me") { header("Authorization", "Bearer $token") }
            .andExpect { status { isOk() } }

        block(email)

        mockMvc.get("/v1/admin/auth/me") { header("Authorization", "Bearer $token") }
            .andExpect { status { isUnauthorized() } }
    }
}
