package com.dbook.presentation.staffmanagement

import org.springframework.test.web.servlet.post
import kotlin.test.Test

class ResendingMailsANewLinkAndKillsTheOldOneTest : StaffManagementFixture() {
    @Test
    fun `given an open invitation when resending then only the newly mailed link works`() {
        val adminToken = newSuperAdminToken()
        val email = uniqueEmail()
        val id = idOf(invite(adminToken, email))
        val oldToken = tokenMailedTo(email)

        mockMvc.post("/v1/admin/invitations/$id/resend") { header("Authorization", "Bearer $adminToken") }
            .andExpect { status { isOk() } }
        val newToken = tokenMailedTo(email)

        accept(oldToken).andExpect { status { isBadRequest() } }
        accept(newToken).andExpect { status { isCreated() } }
    }
}
