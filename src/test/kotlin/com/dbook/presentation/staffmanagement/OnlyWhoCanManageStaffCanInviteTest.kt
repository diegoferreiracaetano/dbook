package com.dbook.presentation.staffmanagement

import com.dbook.domain.identity.Role
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.Test

class OnlyWhoCanManageStaffCanInviteTest : StaffManagementFixture() {
    @Test
    fun `given a support member when inviting or listing then it is 403, and without a token it is 401`() {
        val supportToken = registerStaffAndLogin(uniqueEmail(), Role.SUPPORT)

        invite(supportToken, uniqueEmail()).andExpect { status { isForbidden() } }
        mockMvc.get("/v1/admin/invitations") { header("Authorization", "Bearer $supportToken") }
            .andExpect { status { isForbidden() } }
        mockMvc.get("/v1/admin/staff") { header("Authorization", "Bearer $supportToken") }
            .andExpect { status { isForbidden() } }
        mockMvc.post("/v1/admin/invitations").andExpect { status { isUnauthorized() } }
    }
}
