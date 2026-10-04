package com.dbook.presentation.securityintegration

import com.dbook.domain.identity.Role
import org.springframework.test.web.servlet.get
import kotlin.test.Test

class ADemotedStaffMemberLosesAdminAccessImmediatelyTest : SecurityIntegrationFixture() {
    @Test
    fun `given a staff token that works when the role is taken away then the very next admin call is 403`() {
        val email = uniqueEmail()
        val token = registerStaffAndLogin(email)

        changeRole(email, Role.CLIENT)

        mockMvc.get("/v1/admin/auth/me") { header("Authorization", "Bearer $token") }
            .andExpect { status { isForbidden() } }
    }
}
