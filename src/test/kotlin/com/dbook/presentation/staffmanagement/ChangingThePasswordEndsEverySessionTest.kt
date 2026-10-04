package com.dbook.presentation.staffmanagement

import org.springframework.test.web.servlet.post
import kotlin.test.Test
import kotlin.test.assertEquals

class ChangingThePasswordEndsEverySessionTest : StaffManagementFixture() {
    @Test
    fun `given a staff member when changing the password then old password and old session stop working`() {
        val adminToken = newSuperAdminToken()
        val email = uniqueEmail()
        onboard(adminToken, email)
        val login = adminLogin(email, "a-long-passphrase-1")

        mockMvc.post("/v1/admin/auth/change-password") {
            header("Authorization", "Bearer ${accessTokenOf(login)}")
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    mapOf("currentPassword" to "a-long-passphrase-1", "newPassword" to "another-long-passphrase-2"),
                )
        }.andExpect { status { isNoContent() } }

        adminRefresh(refreshCookieOf(login)).andExpect { status { isUnauthorized() } }
        assertEquals(401, adminLogin(email, "a-long-passphrase-1").response.status)
        assertEquals(200, adminLogin(email, "another-long-passphrase-2").response.status)
    }
}
