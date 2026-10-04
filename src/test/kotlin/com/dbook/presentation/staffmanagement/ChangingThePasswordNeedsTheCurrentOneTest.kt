package com.dbook.presentation.staffmanagement

import org.springframework.test.web.servlet.post
import kotlin.test.Test

class ChangingThePasswordNeedsTheCurrentOneTest : StaffManagementFixture() {
    @Test
    fun `given a wrong current password when changing then it is 401 INVALID_CREDENTIALS and the password stays`() {
        val email = uniqueEmail()
        val token = registerStaffAndLogin(email)

        mockMvc.post("/v1/admin/auth/change-password") {
            header("Authorization", "Bearer $token")
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    mapOf("currentPassword" to "not-my-password", "newPassword" to "another-long-passphrase-2"),
                )
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("INVALID_CREDENTIALS") }
        }

        kotlin.test.assertEquals(200, adminLogin(email, "s3cret-password").response.status)
    }
}
