package com.dbook.presentation.staffmanagement

import com.dbook.domain.identity.Role
import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals

class AnInvitedPersonBecomesAStaffMemberAndLogsInToThePortalTest : StaffManagementFixture() {
    @Test
    fun `given an invitation when it is accepted then the invitee logs in to the portal with the invited role`() {
        val adminToken = newSuperAdminToken()
        val email = uniqueEmail()

        invite(adminToken, email, Role.CATALOG_MANAGER).andExpect {
            status { isCreated() }
            jsonPath("$.status") { value("PENDING") }
            jsonPath("$.role") { value("CATALOG_MANAGER") }
        }
        accept(tokenMailedTo(email), name = "Maria").andExpect {
            status { isCreated() }
            jsonPath("$.role") { value("CATALOG_MANAGER") }
        }

        val login = adminLogin(email, "a-long-passphrase-1")
        assertEquals(200, login.response.status)
        mockMvc.get("/v1/admin/auth/me") { header("Authorization", "Bearer ${accessTokenOf(login)}") }
            .andExpect { jsonPath("$.role") { value("CATALOG_MANAGER") } }
    }
}
